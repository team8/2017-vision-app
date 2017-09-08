package com.frc8.team8vision;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.audiofx.BassBoost;
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
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static org.opencv.imgproc.Imgproc.contourArea;

/**
 * The app's startup activity, as suggested by its name. Handles all
 * camera operations and vision processing.
 * @author Calvin Yan
 */
public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

	private static final String TAG = Constants.kTAG+"MainActivity";

	private static Mat imageRGB;
	private static Mat imageRGB_raw;

	private static double turnAngle = 0;
	private static Double xDist = null, zDist = null;
	private static long cycleTime = 0;

	private MatOfDouble distCoeffs;

	private Mat intrinsicMatrix, imageHSV;

	private static SketchyCameraView mCameraView;
	private static boolean isSettingsPaused = false;

	private long lastCycleTimestamp = 0;

	private int mWidth = 0, mHeight = 0, mPPI = 0;
	private int[] sliderValues = new int[6];
	private int mResolutionFactor = 3;      // Divides screen images by given factor

	private boolean trackingLeft;

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
		trackingLeft = SettingsActivity.trackingLeftTarget();

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
		isSettingsPaused = false;
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
		setSliderValues();
		WriteDataThread.getInstance().resume();
		JPEGStreamerThread.getInstance().resume();
	}

	public void onDestroy() {
		WriteDataThread.getInstance().destroy();
		JPEGStreamerThread.getInstance().destroy();

		super.onDestroy();

		if (mCameraView != null) {
			mCameraView.disableView();
			if (imageHSV != null) imageHSV.release();
		}
	}

	@Override
	public void onCameraViewStarted(int width, int height) {
		mWidth = width;
		mHeight = height;

		// Reduce exposure and turn on flashlight - to be used with reflective tape
		mCameraView.setParameters();
		mCameraView.toggleFlashLight(SettingsActivity.flashlightOn());

		if (!this.isFocusLocked() || !isSettingsPaused) {
			WriteDataThread.getInstance().resume();
			JPEGStreamerThread.getInstance().resume();
		}
	}

	@Override
	public void onCameraViewStopped() {}

	/**
	 * Automatically called before each image frame is displayed. This is where
	 * the app begins to process the image
	 */
	@Override
	public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
		synchronized (this) {
			imageRGB = inputFrame.rgba();
			if (!isGalaxy()) Core.flip(imageRGB, imageRGB, -1); // Necessary because Nexus camera feed is inverted
			imageRGB = track(imageRGB);
			imageRGB_raw = imageRGB.clone();
		}

		imageHSV.release();
		// The returned image will be displayed on screen
		return imageRGB;
	}

	/**
	 * Detects the vision targets through HSV filtering and calls getPosePnP to
	 * estimate camera pose.
	 *
	 * The app detects vision targets by applying an HSV threshold to the image.
	 * This takes a range of possible hues, a range of possible saturations, and
	 * a range of possible values in the HSV colorspace, and finds all pixels that
	 * fall in these possible values. The result is a black and white image where
	 * pixels in the range are white and others are black.
	 *
	 * @param input - the image captured by the camera
	 * @return input - the modified image to show results of processing
	 */
	public Mat track(Mat input) {
		// Calculates time between method calls; this shows the amount of lag
		if (lastCycleTimestamp != 0) cycleTime = System.currentTimeMillis() - lastCycleTimestamp;
		lastCycleTimestamp = System.currentTimeMillis();

		Mat mask = new Mat();

		// Create mask from hsv threshold
		Scalar lower_bound = new Scalar(sliderValues[0], sliderValues[1], sliderValues[2]),
				upper_bound = new Scalar(sliderValues[3], sliderValues[4], sliderValues[5]);
		Imgproc.cvtColor(input, imageHSV, Imgproc.COLOR_RGB2HSV);
		Core.inRange(imageHSV, lower_bound, upper_bound, mask);

		// Tuning mode displays the result of the threshold
		if (SettingsActivity.tuningMode()) {
			Core.normalize(mask, mask, 0, 255, Core.NORM_MINMAX, input.type(), new Mat());
			Core.convertScaleAbs(mask, mask);
			return mask;
		}

		// Find the peg target contour
		ArrayList<MatOfPoint> contours = new ArrayList<>();
		Imgproc.findContours(mask, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
		if (contours.isEmpty()) return input;
		MatOfPoint contour = bestContour(contours, input);

		if (contour != null) {
			Point[] corners = getCorners(contour);
			getPosePnP(corners, input);

			// Draw corners on image
			for (int i = 0; i < corners.length; i++) {
				Imgproc.circle(input, corners[i], 5, new Scalar((corners.length > 1) ? 255/(corners.length-1) * i : 0, 0, 0), -1);
			}

			double ratio = Math.max(corners[1].x - corners[0].x, corners[3].x - corners[2].x)/2;
			double target = (trackingLeft) ? corners[0].x + (Constants.kVisionTargetWidth/2) * ratio
																	: corners[1].x - (Constants.kVisionTargetWidth/2) * ratio;

			Imgproc.circle(input, new Point(target, mHeight/2), 5, new Scalar(0, 0, 255), -1);
			xDist = (target - mWidth/2) / ratio + SettingsActivity.getNexusShift();

		} else {
			xDist = null;
		}

		String printval = "<"+
				(xDist != null ? String.format(Locale.getDefault(), "%.2f", xDist.doubleValue()) : "NaN") + ", " +
				(zDist != null ? String.format(Locale.getDefault(), "%.2f", zDist.doubleValue()) : "NaN") + ">";
		Imgproc.putText(input, printval, new Point(0, mHeight - 30),
				Core.FONT_HERSHEY_SIMPLEX, 2.5/mResolutionFactor, new Scalar(0, 255, 0), 3);
		Imgproc.putText(input, Double.toString(cycleTime),
				new Point(mWidth - 200/mResolutionFactor, mHeight - 30),
                Core.FONT_HERSHEY_SIMPLEX, 2.5/mResolutionFactor, new Scalar(0, 255, 0), 3);
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
		double width = Constants.kVisionTargetWidth, height = Constants.kVisionTargetHeight,
				tapeWidth = Constants.kTapeWidth, depth = Constants.kPegLength, conv = 0.0393701 * 12 / 1.95;
		double leftX, topY, rightX, bottomY;
		if(trackingLeft){
			leftX = -width/2;
			topY = height/2;
			rightX = leftX+tapeWidth;
			bottomY = -height/2;
		}else{
			rightX = width/2;
			topY = height/2;
			leftX = rightX-tapeWidth;
			bottomY = -height/2;
		}
		MatOfPoint2f dstPoints = new MatOfPoint2f();
        dstPoints.fromArray(src);
        // In order to calculate the pose, we create a model of the vision targets using 3D coordinates
        MatOfPoint3f srcPoints = new MatOfPoint3f(new Point3(leftX, topY, 0),
                                                new Point3(rightX, topY, 0),
                                                new Point3(leftX, bottomY, 0),
                                                new Point3(rightX, bottomY, 0));
        MatOfDouble rvecs = new MatOfDouble(), tvecs = new MatOfDouble();
        Calib3d.solvePnP(srcPoints, dstPoints, intrinsicMatrix, distCoeffs, rvecs, tvecs);
		zDist = (tvecs.get(2, 0)[0]) * conv;

        MatOfPoint3f newPoints = new MatOfPoint3f(
        		new Point3(0, 0, zDist),
				new Point3(0, 0, 0)
		);

        MatOfPoint2f result = new MatOfPoint2f();
        Calib3d.projectPoints(newPoints, rvecs, tvecs, intrinsicMatrix, distCoeffs, result);
        Point[] arr = result.toArray();

        // Estimates the position of the base and tip of the peg
        Imgproc.line(input, arr[0], arr[1], new Scalar(255, 255, 255), 5);

		for(Point p: arr){
			Imgproc.circle(input, p, 7, new Scalar(0,255,0));
		}
    }


	/**
	 * Remove all contours that are below a certain area threshold. Used to remove salt noise.
	 */
	private MatOfPoint bestContour(ArrayList<MatOfPoint> contours, Mat input) {
		// Sort contours in decreasing order of area
		Collections.sort(contours, new Comparator<MatOfPoint>() {
			public int compare(MatOfPoint one, MatOfPoint two) {
				return Double.compare(Imgproc.contourArea(two), Imgproc.contourArea(one));
			}
		});
		double threshold = 0.4 * Imgproc.contourArea(contours.get(0));

		List<MatOfPoint> found = new ArrayList<MatOfPoint>();
		for (MatOfPoint contour: contours) {
			if (Imgproc.contourArea(contour) < threshold) found.add(contour);
		}
		contours.removeAll(found);
		Imgproc.drawContours(input, found, -1, new Scalar(0, 0, 255));

		// Were both strips of tape detected?
		if (contours.size() >= 2) {
			Imgproc.drawContours(input, contours, 0, new Scalar(255, 0, 0));
			Imgproc.drawContours(input, contours, 1, new Scalar(0, 255, 0));
			// Calculate bounding rectangles of contours to compare x position
			Rect oneRect = Imgproc.boundingRect(contours.get(0)), twoRect = Imgproc.boundingRect(contours.get(1));
			double oneArea = oneRect.area(), twoArea = twoRect.area();

			if (oneArea<=twoArea){
				trackingLeft = oneRect.x >= twoRect.x;
				contours.remove(0);
			} else {
				trackingLeft = oneRect.x < twoRect.x;
			}
		}

		SettingsActivity.setTrackingLeft(trackingLeft);

		// If no target found
		if (contours.size() == 0){
			return null;
		}

		// The first contour should be the optimal one
		return contours.get(0);
	}

	/**
	 * Identify four corners of the contour. Sketchy implementation but it works.
	 */
	private Point[] getCorners(MatOfPoint contour) {
		Point[] arr = contour.toArray(), retval = new Point[4];
		Arrays.sort(arr, new Comparator<Point>() {
			public int compare(Point p1, Point p2) {
				return (int)(p1.x + p1.y - (p2.x + p2.y));
			}
		});
		retval[0] = arr[0];
		retval[3] = arr[arr.length-1];
		Arrays.sort(arr, new Comparator<Point>() {
			public int compare(Point p1, Point p2) {
				return (int)(p1.x - p1.y - (p2.x - p2.y));
			}
		});
		retval[2] = arr[0];
		retval[1] = arr[arr.length-1];
		return retval;
	}

	private void setSliderValues(){
		// Retrieve HSV threshold stored in app, see SettingsActivity.java for more info
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		for (int i = 0; i < 6; i++) {
			sliderValues[i] = preferences.getInt(Constants.kSliderNames[i], Constants.kSliderDefaultValues[i]);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_main, menu);
		return true;
	}

	public void launchSetThresholdActivity(MenuItem item) {
		isSettingsPaused = true;
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
		if (imageRGB_raw != null && imageRGB_raw.channels()<=4){
			Imgproc.cvtColor(imageRGB_raw, imageRGB_raw, Imgproc.COLOR_BGRA2RGBA);
		}
		return imageRGB_raw;
	}
	public static double getTurnAngle() { return turnAngle; }
	public static Double getXDisplacement() {
		if(xDist == null || xDist.isNaN() || xDist.isInfinite()) {
			return null;
		}else{
			return xDist;
		}
	}
	public static Double getZDisplacement() {
		if(zDist == null || zDist.isNaN() || zDist.isInfinite()) {
			return null;
		}else{
			return zDist;
		}
	}
	public static long getCycleTime() { return cycleTime; }
	public boolean isFocusLocked(){
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		int lockValue = preferences.getInt("Focus Lock Value", 0);
		return lockValue >= 80;
	}

	public static void toggleFlash(boolean flashlightOn) {
		SettingsActivity.setFlashlightOn(flashlightOn);
		mCameraView.toggleFlashLight(flashlightOn);
	}
}
