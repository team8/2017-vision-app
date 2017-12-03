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

    protected MainActivity mMainActivity;
    protected CameraManager mCameraManager;
    protected CameraDevice mCameraDevice;
    protected Mat mYuv420888Mat;
    protected CameraFrame mFrame;
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

            imageToMat(image, mYuv420888Mat);

//            Image.Plane Y = image.getPlanes()[0];
//            Image.Plane U = image.getPlanes()[1];
//            Image.Plane V = image.getPlanes()[2];

//
//            int Yb = Y.getBuffer().remaining();
//            int Ub = U.getBuffer().remaining();
//            int Vb = V.getBuffer().remaining();
//
//            byte[] data = new byte[Yb + Ub + Vb];
//
//            Y.getBuffer().get(data, 0, Yb);
//            U.getBuffer().get(data, Yb, Ub);
//            V.getBuffer().get(data, Yb + Ub, Vb);
//
//            mYuv420888Mat.put(0, 0, data);

            //Log.i(kTAG, String.format("%d, %d, %d", Yb, Ub, Vb));

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

                                mPreviewSize = new Size(480, 640);

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

                            mFrameHeight = mPreviewSize.getHeight();
                            mFrameWidth  = mPreviewSize.getWidth ();

                            //mScale = Math.min((float)height/mFrameHeight, (float)width/mFrameWidth);
                            mScale = 2.25f;

                            mYuv420888Mat = new Mat(mFrameHeight + mFrameHeight/2, mFrameWidth, CvType.CV_8UC1);

                            mFrame = new CameraFrame(mYuv420888Mat, mFrameWidth, mFrameHeight);

                            mImageReader = ImageReader.newInstance(mFrameWidth, mFrameHeight, ImageFormat.YUV_420_888, 1);
                            mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);

                            AllocateCache();

                            mCameraManager.openCamera(id, mStateCallback, mBackgroundHandler);
                        }
                    }
                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private void createCameraPreviewSession() {

        try {

            mSurfaceTexture.setDefaultBufferSize(mFrameWidth, mFrameHeight);

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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == 1) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Log.e(kTAG, "Could not get permission for camera!");
            }
        } else {
            mMainActivity.onRequestPermissionsResult(requestCode, permissions, grantResults);
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

        private Mat mYuv420888, mBgr, mRgba, mRotated;

        @Override
        public Mat gray() {

            return null;
        }

        @Override
        public Mat rgba() {

            Imgproc.cvtColor(mYuv420888, mBgr, Imgproc.COLOR_YUV2BGR_I420);
            Imgproc.cvtColor(mBgr, mRgba, Imgproc.COLOR_BGR2RGBA, 0);
//            if (mRotated != null) mRotated.release();
//            mRotated = mRgba.t();
//            Core.flip(mRotated, mRotated, 1);
            return mRgba;
        }

        public CameraFrame(final Mat Yuv420888, final int imageWidth, final int imageHeight) {

            super();

            mYuv420888 = Yuv420888;

            mBgr = new Mat();
            mRgba = new Mat();
            mRotated = new Mat();
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

    /**
     * Takes an {@link Image} in the YUV_420_888 and puts it into a provided {@link Mat}
     *
     * @param image {@link Image} in the YUV_420_888 format.
     */
    public static void imageToMat(Image image, Mat mat) {

        ByteBuffer buffer;
        int rowStride;
        int pixelStride;
        int width = image.getWidth();
        int height = image.getHeight();
        int offset = 0;

        Image.Plane[] planes = image.getPlanes();
        byte[] data = new byte[image.getWidth() * image.getHeight() * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8];
        byte[] rowData = new byte[planes[0].getRowStride()];

        for (int i = 0; i < planes.length; i++) {
            buffer = planes[i].getBuffer();
            rowStride = planes[i].getRowStride();
            pixelStride = planes[i].getPixelStride();
            int w = (i == 0) ? width : width / 2;
            int h = (i == 0) ? height : height / 2;
            for (int row = 0; row < h; row++) {
                int bytesPerPixel = ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8;
                if (pixelStride == bytesPerPixel) {
                    int length = w * bytesPerPixel;
                    buffer.get(data, offset, length);

                    // Advance buffer the remainder of the row stride, unless on the last row.
                    // Otherwise, this will throw an IllegalArgumentException because the buffer
                    // doesn't include the last padding.
                    if (h - row != 1) {
                        buffer.position(buffer.position() + rowStride - length);
                    }
                    offset += length;
                } else {

                    // On the last row only read the width of the image minus the pixel stride
                    // plus one. Otherwise, this will throw a BufferUnderflowException because the
                    // buffer doesn't include the last padding.
                    if (h - row == 1) {
                        buffer.get(rowData, 0, width - pixelStride + 1);
                    } else {
                        buffer.get(rowData, 0, rowStride);
                    }

                    for (int col = 0; col < w; col++) {
                        data[offset++] = rowData[col * pixelStride];
                    }
                }
            }
        }

        mat.put(0, 0, data);
    }
}
