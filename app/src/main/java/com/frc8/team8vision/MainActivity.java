package com.frc8.team8vision;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.RadioButton;
import android.widget.Switch;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

/**
 * The app's startup activity, as suggested by its name. Handles all
 * camera operations and vision processing.
 * @author Calvin Yan
 */
public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "MainActivity";

    private static Mat imageRGB;
    private static Mat imageRGB_raw;
    // Lock for synchronized access of imageRGB
    private final Object lock = new Object();

    private static double turnAngle = 0, xDist = 0;
    private static long cycleTime = 0;

    private MatOfDouble distCoeffs;

    private Mat intrinsicMatrix, imageHSV;

    private static SketchyCameraView mCameraView;

    private long lastCycleTimestamp = 0;

    private int mWidth = 0, mHeight = 0, mPPI = 0;
    private int mResolutionFactor = 3;      // Divides screen images by given factor

    /**
     * The delay between starting the app and loading OpenCV libraries means that
     * all OpenCV objects must be instantiated in this callback function.
     */
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV load success");
                    imageHSV = new Mat();
                    // Start camera feed
                    mCameraView.enableView();

                    /*
                     * Load intrinsic matrix and distortion coefficients of camera;
                     * used for pose estimation
                     */

                    intrinsicMatrix = new Mat(3, 3, CvType.CV_64F);
                    distCoeffs = new MatOfDouble();

                    if (isGalaxy()) {
                        // Galaxy S4 is being used
                        for (int i = 0; i < 3; i++) {
                            for (int j = 0; j < 3; j++) {
                                intrinsicMatrix.put(i, j, Constants.kGalaxyIntrinsicMatrix[i][j]);
                            }
                        }
                        distCoeffs.fromArray(Constants.kGalaxyDistortionCoefficients);
                        mPPI = Constants.kGalaxyPixelsPerInch;
                    } else {
                        // Nexus 5x is being used
                        for (int i = 0; i < 3; i++) {
                            for (int j = 0; j < 3; j++) {
                                intrinsicMatrix.put(i, j, Constants.kNexusIntrinsicMatrix[i][j]);
                            }
                        }
                        distCoeffs.fromArray(Constants.kNexusDistortionCoefficients);
                        mPPI = Constants.kNexusPixelsPerInch;
                    }
                } break;
                default: {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCameraView = new SketchyCameraView(this, -1);
        setContentView(mCameraView);
        mCameraView.setCvCameraViewListener(this);
        mCameraView.setMaxFrameSize(1920/ mResolutionFactor,1080/ mResolutionFactor);

        WriteDataThread.getInstance().start(this, WriteDataThread.WriteState.JSON);
        JPEGStreamerThread.getInstance().start(this);
    }

    @Override
    public void onPause() {
        JPEGStreamerThread.getInstance().pause();
        WriteDataThread.getInstance().pause();
        super.onPause();

        if (mCameraView != null) {
            mCameraView.disableView();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();

        if (mCameraView != null) {
            mCameraView.disableView();
            if (imageHSV != null) imageHSV.release();
        }
        WriteDataThread.getInstance().destroy();
        JPEGStreamerThread.getInstance().destroy();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mWidth = width;
        mHeight = height;

        // Reduce exposure and turn on flashlight - to be used with reflective tape
        mCameraView.setParameters();
        if (SettingsActivity.flashlightOn()) mCameraView.toggleFlashLight();

        WriteDataThread.getInstance().resume();
        JPEGStreamerThread.getInstance().resume();
    }

    @Override
    public void onCameraViewStopped() {
    }

    @Override
    /**
     * Automatically called before each image frame is displayed. This is where
     * the app begins to process the image
     */
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        synchronized (lock) {
            imageRGB_raw = inputFrame.rgba().clone();
			Imgproc.cvtColor(imageRGB_raw, imageRGB_raw, Imgproc.COLOR_RGBA2BGRA);
            imageRGB = inputFrame.rgba();
			if (isGalaxy()) Core.flip(imageRGB, imageRGB, -1); // Necessary because Galaxy camera feed is inverted
            imageRGB = track(imageRGB);
        }

        imageHSV.release();
        // The returned image will be displayed on screen
        return imageRGB;
    }

    /**
     * Detects the vision targets through HSV filtering and calls getPosePnP to
     * estimate camera pose.
     * @param input - the image captured by the camera
     * @return input - the modified image to show results of processing
     */
    public Mat track(Mat input) {
        // Calculates time between method calls; this shows the amount of lag
        if (lastCycleTimestamp != 0) cycleTime = System.currentTimeMillis() - lastCycleTimestamp;
        lastCycleTimestamp = System.currentTimeMillis();

        // Retrieve HSV threshold stored in app, see SettingsActivity.java for more info
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        int[] sliderValues = new int[6];
        for (int i = 0; i < 6; i++) {
            sliderValues[i] = preferences.getInt(Constants.kSliderNames[i], Constants.kSliderDefaultValues[i]);
        }

        /*
         * The app detects vision targets by applying an HSV threshold to the image.
         * This takes a range of possible hues, a range of possible saturations, and
         * a range of possible values in the HSV colorspace, and finds all pixels that
         * fall in these possible values. The result is a black and white image where
         * pixels in the range are white and others are black.
         */
        Mat mask = new Mat();

        // Lower and upper hsv thresholds
        Scalar lower_bound = new Scalar(sliderValues[0], sliderValues[1], sliderValues[2]),
                upper_bound = new Scalar(sliderValues[3], sliderValues[4], sliderValues[5]);

        // Convert RGB to HSV
        Imgproc.cvtColor(input, imageHSV, Imgproc.COLOR_RGB2HSV);
        // Apply threshold
        Core.inRange(imageHSV, lower_bound, upper_bound, mask);

        // Morphological opening removes salt noise
		Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_OPEN, Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, new Size(9, 9)));
		// Normalize and scale binary mask so it can be displayed
        Core.normalize(mask, mask, 0, 255, Core.NORM_MINMAX, input.type(), new Mat());
		Core.convertScaleAbs(mask, mask);

        // Setting this to 0 returns the HSV threshold result - temporary implementation
		int code = 1;
        if (SettingsActivity.tuningMode()) {
            return mask;
        }


        // Detect corners on vision target using Harris Corner Detector
        MatOfPoint cornerMat = new MatOfPoint();
        Imgproc.goodFeaturesToTrack(mask, cornerMat, 8, .1, 10, new Mat(), 7, true, .05);
        Point[] corners = cornerMat.toArray();

        /*
         * Sort corners based on the sum of their x and y coordinates. This is to ensure
         * that they are always in the same order. Sketchy, but it works
         */
        Arrays.sort(corners, new Comparator<Point>() {
            public int compare(Point p1, Point p2) {
                return (int)((p1.x + p1.y) - (p2.x + p2.y));
            }
        });

        // Draw corners on image
        for (int i = 0; i < corners.length; i++) {
            Imgproc.circle(input, corners[i], 15, new Scalar((corners.length > 1) ? 255/(corners.length-1) * i : 0, 0, 0), -1);
        }

        if ((corners.length == 4)) {
            // Compute the three rotations and three translations of the camera
            getPosePnP(corners, input);

            Imgproc.putText(input,
                    String.format(Locale.getDefault(), "%.2f",
                    xDist), new Point(0, mHeight - 30),
                    Core.FONT_HERSHEY_SIMPLEX, 3/mResolutionFactor, new Scalar(0, 255, 0), 3);
        }
        Imgproc.putText(input,
                Double.toString(cycleTime), new Point(mWidth - 200, mHeight - 30),
                Core.FONT_HERSHEY_SIMPLEX, 3/mResolutionFactor, new Scalar(0, 255, 0), 3);
        return input;
    }

    /**
     * Determines the transformation of a camera relative to the vision target.
     * The transformation is broken down into rotations and translations along the x, y, and z axes.
     * Note that the rotation values are accurate while the translations are not - I'm still trying
     * to figure out how to use the middle of the target as the reference point, instead of the top
     * left corner.
     * @param src - corners of the image contained in an array
     * @param input - the image captured by the camera
     */
    public void getPosePnP(Point[] src, Mat input) {
        double scalar = mPPI, width = Constants.kTapeWidth * scalar, height = Constants.kVisionTargetHeight * scalar;
        double leftX = (mWidth - width) / 2, topY = (mHeight - height) / 2, rightX = leftX + width, bottomY = topY + height, z = 0 - 2 * scalar;
        MatOfPoint2f dstPoints = new MatOfPoint2f();
        dstPoints.fromArray(src);
        // In order to calculate the pose, we create a model of the vision targets using 3D coordinates
        MatOfPoint3f srcPoints = new MatOfPoint3f(new Point3(leftX, topY, 0),
                                                new Point3(rightX, topY, 0),
                                                new Point3(leftX, bottomY, 0),
                                                new Point3(rightX, bottomY, 0));
        MatOfDouble rvecs = new MatOfDouble(), tvecs = new MatOfDouble();
        Calib3d.solvePnP(srcPoints, dstPoints, intrinsicMatrix, distCoeffs, rvecs, tvecs);

        /*
         * What x position is the peg supposed to be at? This depends on which
         * target is being tracked - right or left.
         */

        double pegX;
        if (SettingsActivity.trackingLeftTarget()) {
            pegX = leftX + Constants.kVisionTargetWidth*scalar/2;
        } else {
            pegX = rightX - Constants.kVisionTargetWidth*scalar/2;
        }

        MatOfPoint3f newPoints = new MatOfPoint3f(new Point3(pegX, (topY+bottomY)/2, 0),
                                                new Point3(pegX, (topY+bottomY)/2, -1 * Constants.kPegLength * scalar));

        MatOfPoint2f result = new MatOfPoint2f();
        Calib3d.projectPoints(newPoints, rvecs, tvecs, intrinsicMatrix, distCoeffs, result);
        Point[] arr = result.toArray();

        // Estimates the position of the base and tip of the peg
        Imgproc.line(input, arr[0], arr[1], new Scalar(255, 255, 255), 5);
        // Convert the z translation to inches and subtract by the peg length to get distance from the peg tip
        double zDist = (tvecs.get(2, 0)[0] + Constants.kGalaxyFocalLengthZ) / 2231 - Constants.kPegLength;
        // Given the perceived x displacement,
        xDist = zDist;
        //xDist = ((arr[1].x - mWidth / 2) * zDist / 4) / mPPI;
        double[] angles = rvecs.toArray();
        turnAngle = Math.toDegrees(angles[1]);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    public void launchSetThresholdActivity(MenuItem item) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    /**
     * A Samsung Galaxy S4 was used for vision testing when the Nexus was unavailable.
     * Its properties are different from the Nexus, so sometimes the app must determine
     * which phone is running it.
     */
    private boolean isGalaxy() { return Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP; }

    // Getter methods allowing the networking utility classes to retrieve data

    public static Mat getImage() {
       return imageRGB_raw;
    }
    public static double getTurnAngle() { return turnAngle; }
    public static double getXDisplacement() { return xDist; }
    public static long getCycleTime() { return cycleTime; }

    public static void toggleFlash() {
		mCameraView.toggleFlashLight();
	}
}
