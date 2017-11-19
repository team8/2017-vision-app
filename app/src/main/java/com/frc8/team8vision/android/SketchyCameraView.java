package com.frc8.team8vision.android;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import com.frc8.team8vision.util.Constants;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.Semaphore;

/**
 * Heavily modified version of OpenCV's camera class that supports portrait mode
 * and flashlight manipulation. Much of this overlaps with JavaCameraView; it will
 * eventually be made a subclass.
 */
public class SketchyCameraView extends CameraBridgeViewBase implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final int MAGIC_TEXTURE_ID = 10;
    private static final int STATE_PREVIEW = 0, STATE_WAITING_LOCK = 1, STATE_WAITING_PRECAPTURE = 2, STATE_WAITING_NON_PRECAPTURE = 3, STATE_PICTURE_TAKEN = 4;
    private static final String TAG = Constants.kTAG+"SketchyCameraView";

    private Image mImage;
    private Mat mImageMat;
    private JavaCameraFrame mFrame;
    private Thread mThread;
    private boolean mStopThread;
    protected MainActivity mMainActivity;
    protected CameraManager mCameraManager;
    protected CameraDevice mCameraDevice;
    protected JavaCameraFrame[] mCameraFrame;
    private String mCameraId;
    private CameraCaptureSession mCaptureSession;
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private Size mPreviewSize;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CaptureRequest mPreviewRequest;
    private int mState = STATE_PREVIEW;
    private CameraCharacteristics mCameraCharacteristics;
    protected Handler   mBackgroundHandler;
    protected HandlerThread mBackgroundThread;
    private SurfaceTexture mSurfaceTexture;
    private ImageReader mImageReader;
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice camera) {

            Log.i(Constants.kTAG, "Camera successfully opened!");
            mCameraOpenCloseLock.release();
            mCameraDevice = camera;

            mStopThread = false;
            mThread = new Thread(new CameraWorker());
            mThread.start();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {

            Log.i(Constants.kTAG, "Camera disconnected.");
            mCameraOpenCloseLock.release();
            mCameraDevice.close();
            mCameraDevice = null;

            try {
                mStopThread = true;
                synchronized (this) {
                    this.notify();
                }
                if (mThread != null)
                    mThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                mThread = null;
            }
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {

            Log.w(Constants.kTAG, "Error with camera! Error:" + error);
        }
    };

    private final CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {

            switch (mState) {
                case STATE_PREVIEW: {
                    break;
                }
                case STATE_WAITING_LOCK: {
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == null) {
                        captureStillPicture();
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null ||
                                aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = STATE_PICTURE_TAKEN;
                            captureStillPicture();
                        } else {
                            runPrecaptureSequence();
                        }
                    }
                    break;
                }
                case STATE_WAITING_PRECAPTURE: {
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mState = STATE_WAITING_NON_PRECAPTURE;
                    }
                    break;
                }
                case STATE_WAITING_NON_PRECAPTURE: {
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    }
                    break;
                }
            }
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            process(result);
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            process(partialResult);
        }
    };

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            mImage = reader.acquireNextImage();
        }
    };

    public SketchyCameraView(MainActivity mainActivity, int cameraId) {

        super(mainActivity, cameraId);

        mMainActivity = mainActivity;

        initializeCamera();
    }

    private void initializeCamera() {

        mCameraManager = mMainActivity.getSystemService(CameraManager.class);

        if (mCameraManager != null) {

            try {
                if (ContextCompat.checkSelfPermission(mMainActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    requestCameraPermission();
                } else {
                    String[] cameraIds = mCameraManager.getCameraIdList();

                    for (String id : cameraIds) {

                        CameraCharacteristics cameraCharacteristics = mCameraManager.getCameraCharacteristics(id);
                        final Integer facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);

                        if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {

                            // Found correct camera (facing back)
                            mCameraId = id;
                            mCameraCharacteristics = cameraCharacteristics;
                            mCameraManager.openCamera(id, mStateCallback, mBackgroundHandler);

                            final StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                            if (map != null) {

                                final Size[] outputSizes = map.getOutputSizes(SurfaceTexture.class);

                                for (Size size : outputSizes) {

                                    if (size.getHeight() == 1080 && size.getWidth() == 1920) {

                                        mPreviewSize = size;
                                    }
                                }
                            }

                            mSurfaceTexture = new SurfaceTexture(MAGIC_TEXTURE_ID);

                            mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.JPEG, 2);
                            mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);

                            mImageMat = new Mat(mPreviewSize.getHeight(), mPreviewSize.getWidth(), CvType.CV_8UC1);

                            mFrame = new JavaCameraFrame(mImageMat, mPreviewSize.getWidth(), mPreviewSize.getHeight());
                        }
                    }
                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private void runPrecaptureSequence() {
        try {
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            mState = STATE_WAITING_PRECAPTURE;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void captureStillPicture() {
        try {
            if (mMainActivity == null || mCameraDevice == null) {
                return;
            }

            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);

            captureBuilder.addTarget(new Surface(mSurfaceTexture));

            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, 270);

            CameraCaptureSession.CaptureCallback CaptureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    try {
                        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                                CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
                        mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                                mBackgroundHandler);
                        mState = STATE_PREVIEW;
                        mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback,
                                mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
            };

            mCaptureSession.stopRepeating();
            mCaptureSession.abortCaptures();
            mCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == 1) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Log.e(Constants.kTAG, "Could not get permission for camera!");
            }
        } else {
            mMainActivity.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private boolean createCameraPreviewSession() {

        try {

            mSurfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            final Surface surface = new Surface(mSurfaceTexture);

            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);

            mCameraDevice.createCaptureSession
            (
                    Arrays.asList(surface),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {

                            mCaptureSession = session;

                            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                            mPreviewRequest = mPreviewRequestBuilder.build();

                            try {

                                mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, mBackgroundHandler);

                            } catch (CameraAccessException e) {

                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                        }
                    },
                    null
            );

            return true;

        } catch (CameraAccessException e) {

            e.printStackTrace();
        }

        return false;
    }

    protected void requestCameraPermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(mMainActivity, Manifest.permission.CAMERA)) {
            new CameraPermissionConfirmationDialog().show(mMainActivity.getFragmentManager(), "dialog");
        } else {
            mMainActivity.requestPermissions(new String[] {Manifest.permission.CAMERA}, 1);
        }
    }

    @Override
    public void enableView() {

        super.enableView();

        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    @Override
    public void disableView() {

        super.disableView();

        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            mCameraOpenCloseLock.acquire();

            if (mCaptureSession != null) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    @Override
    protected boolean connectCamera(int width, int height) {

        return true;
    }

    @Override
    protected void disconnectCamera() {

    }

    public void setParameters() {

    }

    protected void setFlashlight(final boolean on) {

    }

    private class JavaCameraFrame implements CvCameraViewFrame {

        private Mat mYuvFrameData;
        private Mat mRgba;
        private int mWidth;
        private int mHeight;
        private Mat mRotated;

        public Mat gray() {

            if (mRotated != null) mRotated.release();
            mRotated = mYuvFrameData.submat(0, mWidth, 0, mHeight); //submat with reversed width and height because its done on the landscape frame
            mRotated = mRotated.t();
            Core.flip(mRotated, mRotated, 1);
            return mRotated;
        }

        public Mat rgba() {

            Imgproc.cvtColor(mYuvFrameData, mRgba, Imgproc.COLOR_YUV2BGR_NV12, 4);
            if (mRotated != null) mRotated.release();
            mRotated = mRgba.t();
            Core.flip(mRotated, mRotated, 1);
            return mRotated;
        }

        public JavaCameraFrame(Mat Yuv420sp, int width, int height) {

            super();
            mWidth = width;
            mHeight = height;
            mYuvFrameData = Yuv420sp;
            mRgba = new Mat();
        }

        public void release() {

            mRgba.release();
            if (mRotated != null) mRotated.release();
        }
    }

    private class CameraWorker implements Runnable {

        @Override
        public void run() {

            do {
                synchronized (SketchyCameraView.this) {
                    try {
                        SketchyCameraView.this.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                //Log.i(Constants.kTAG, "Yeeeet");

                ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);

                mImageMat.put(0, 0, bytes);

                if (!mStopThread) {
                    deliverAndDrawFrame(mFrame);
                }
            } while (!mStopThread);
        }
    }
}
