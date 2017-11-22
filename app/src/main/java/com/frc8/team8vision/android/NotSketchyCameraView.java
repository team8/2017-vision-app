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

public class NotSketchyCameraView extends CameraBridgeViewBase implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final int MAGIC_TEXTURE_ID = 10;
    private static final String kTAG = Constants.kTAG + "SketchyCameraView";

    private Mat mImageMat;
    private CameraFrame mFrame;
    protected MainActivity mMainActivity;
    protected CameraManager mCameraManager;
    protected CameraDevice mCameraDevice;
    private String mCameraId;
    private CameraCaptureSession mCaptureSession;
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private Size mPreviewSize;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CaptureRequest mPreviewRequest;
    private CameraCharacteristics mCameraCharacteristics;
    protected Handler mBackgroundHandler;
    protected HandlerThread mBackgroundThread;
    private SurfaceTexture mSurfaceTexture;
    private ImageReader mImageReader;

    private final CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {

        }
    };

    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice camera) {

            Log.i(kTAG, String.format("Camera successfully opened with id: %s!", mCameraId));
            mCameraOpenCloseLock.release();
            mCameraDevice = camera;

            createCameraPreviewSession();

//            mStopThread = false;
//            mWorkerThread = new Thread(new CameraWorker());
//            mWorkerThread.start();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {

            Log.i(kTAG, String.format("Camera with id: %s disconnected.", mCameraId));
            mCameraOpenCloseLock.release();
            mCameraDevice.close();
            mCameraDevice = null;

//            try {
//                mStopThread = true;
//                synchronized (this) {
//                    this.notify();
//                }
//                if (mWorkerThread != null)
//                    mWorkerThread.join();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            } finally {
//                mWorkerThread = null;
//            }
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {

            mCameraOpenCloseLock.release();
            mCameraDevice.close();
            mCameraDevice = null;

            Log.w(Constants.kTAG, String.format("Error with camera! Error: %d", error));
        }
    };

//    private final CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
//
//        private void process(CaptureResult result) {
//
//            Log.i(kTAG, Integer.toString(mState));
//
//
//            switch (mState) {
//                case STATE_PREVIEW: {
//                    break;
//                }
//                case STATE_WAITING_LOCK: {
//                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
//                    if (afState == null) {
//                        captureStillPicture();
//                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
//                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
//                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
//                        if (aeState == null ||
//                                aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
//                            mState = STATE_PICTURE_TAKEN;
//                            captureStillPicture();
//                        } else {
//                            runPrecaptureSequence();
//                        }
//                    }
//                    break;
//                }
//                case STATE_WAITING_PRECAPTURE: {
//                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
//                    if (aeState == null ||
//                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
//                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
//                        mState = STATE_WAITING_NON_PRECAPTURE;
//                    }
//                    break;
//                }
//                case STATE_WAITING_NON_PRECAPTURE: {
//                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
//                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
//                        mState = STATE_PICTURE_TAKEN;
//                        captureStillPicture();
//                    }
//                    break;
//                }
//            }
//        }
//
//        @Override
//        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
//            process(result);
//        }
//
//        @Override
//        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
//            process(partialResult);
//        }
//    };

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {

            Image image = reader.acquireNextImage();

            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);

            mImageMat.put(0, 0, bytes);

            deliverAndDrawFrame(mFrame);

            image.close();
        }
    };

//    public static class CameraSizeAccessor implements ListItemAccessor {
//
//        @Override
//        public int getWidth(Object object) {
//
//            return ((Size)object).getWidth();
//        }
//
//        @Override
//        public int getHeight(Object object) {
//
//            return ((Size)object).getWidth();
//        }
//    }

    public NotSketchyCameraView(MainActivity mainActivity, int cameraId) {

        super(mainActivity, cameraId);

        mMainActivity = mainActivity;
    }

    private void initializeCamera(final int width, final int height) {

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

                            final StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                            if (map != null) {

//                                final Size[] outputSizes = map.getOutputSizes(SurfaceTexture.class);
//
//                                final org.opencv.core.Size frameSize = calculateCameraFrameSize(Arrays.asList(outputSizes), new CameraSizeAccessor(), width, height);
//
//                                mPreviewSize = new Size((int)frameSize.width, (int)frameSize.height);

                                mPreviewSize = new Size(1080, 1920);

                                Log.i(kTAG, mPreviewSize.toString());

//                                for (Size size : outputSizes) {
//
//                                    Log.i(kTAG, size.toString());
//
//                                    if (size.getHeight() == height && size.getWidth() == width) {
//
//                                        Log.i(kTAG, String.format("Camera preview size: %s", size.toString()));
//
//                                        mPreviewSize = size;
//                                    }
//                                }
                            }

                            mSurfaceTexture = new SurfaceTexture(MAGIC_TEXTURE_ID);

                            mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.YUV_420_888, 1);
                            mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);

                            mImageMat = new Mat(mPreviewSize.getHeight(), mPreviewSize.getWidth(), CvType.CV_8UC1);

                            mFrame = new CameraFrame(mImageMat, mPreviewSize.getWidth(), mPreviewSize.getHeight());

                            mCameraManager.openCamera(id, mStateCallback, mBackgroundHandler);
                        }
                    }
                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == 1) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Log.e(kTAG, "Could not get permission for camera!");
            }
        } else {
            mMainActivity.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void createCameraPreviewSession() {

        try {

            mSurfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            final Surface surface = new Surface(mSurfaceTexture), imageReaderSurface = mImageReader.getSurface();

            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);
            mPreviewRequestBuilder.addTarget(imageReaderSurface);

            mCameraDevice.createCaptureSession
            (
                    Arrays.asList(surface, imageReaderSurface),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {

                            mCaptureSession = session;

                            //mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                            mPreviewRequest = mPreviewRequestBuilder.build();

                            try {

                                mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, mBackgroundHandler);

                                Log.i(kTAG, "Capture session created!");

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

        } catch (CameraAccessException e) {

            e.printStackTrace();
        }
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

        Log.i(kTAG, "View enabled.");

        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    @Override
    public void disableView() {

        super.disableView();

        Log.i(Constants.kTAG, "View disabled.");

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

        initializeCamera(width, height);

        return true;
    }

    @Override
    protected void disconnectCamera() {

        if (mCameraDevice != null)
            mCameraDevice.close();
    }

    public void setParameters() {

    }

    protected void setFlashlight(final boolean on) {

    }

    private class CameraFrame implements CvCameraViewFrame {

        private Mat mYuvFrameData, mRgba;
        private int mWidth, mHeight;
        private Mat mRotated;

        @Override
        public Mat gray() {

            if (mRotated != null) mRotated.release();
            mRotated = mYuvFrameData.submat(0, mWidth, 0, mHeight); //submat with reversed width and height because its done on the landscape frame
            mRotated = mRotated.t();
            Core.flip(mRotated, mRotated, 1);
            return mRotated;
        }

        @Override
        public Mat rgba() {

            Imgproc.cvtColor(mYuvFrameData, mRgba, Imgproc.COLOR_YUV2BGR_NV12, 4);
            if (mRotated != null) mRotated.release();
            mRotated = mRgba.t();
            Core.flip(mRotated, mRotated, 1);
            return mRotated;
        }

        public CameraFrame(Mat Yuv420sp, int width, int height) {

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

//    private class CameraWorker implements Runnable {
//
//        @Override
//        public void run() {
//
//            do {
//                synchronized (NotSketchyCameraView.this) {
//                    try {
//                        NotSketchyCameraView.this.wait();
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//
//                ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
//                byte[] bytes = new byte[buffer.remaining()];
//                buffer.get(bytes);
//
//                mImage.close();
//
//                mImageMat.put(0, 0, bytes);
//
//                deliverAndDrawFrame(mFrame);
//
//            } while (!mStopThread);
//        }
//    }
}
