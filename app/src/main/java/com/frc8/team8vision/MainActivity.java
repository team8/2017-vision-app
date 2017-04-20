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

	private static final String TAG = "MainActivity";

	private static Mat imageRGB;
	private static Mat imageRGB_raw;
	// Lock for synchronized access of imageRGB
	private final Object lock = new Object();

	private static double turnAngle = 0;
	private static Double xDist = null;
	private static long cycleTime = 0;

	private MatOfDouble distCoeffs;

	private Mat intrinsicMatrix, imageHSV;

	private static SketchyCameraView mCameraView;

	private long lastCycleTimestamp = 0;

	private int mWidth = 0, mHeight = 0, mPPI = 0;
	private double lastZ, lastZ_1, lastX, lastX_1;
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
		mCameraView.toggleFlashLight(SettingsActivity.flashlightOn());

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
			imageRGB = inputFrame.rgba();
			if (!isGalaxy()) Core.flip(imageRGB, imageRGB, -1); // Necessary because Nexus camera feed is inverted
			imageRGB = track(imageRGB);
			imageRGB_raw = imageRGB.clone();
			if (imageRGB_raw.channels() == 5){
				Imgproc.cvtColor(imageRGB_raw, imageRGB_raw, Imgproc.COLOR_RGBA2BGRA);
			}
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

		Imgproc.putText(input,
				Double.toString(cycleTime), new Point(mWidth - 200, mHeight - 30),
				Core.FONT_HERSHEY_SIMPLEX, 3/mResolutionFactor, new Scalar(0, 255, 0), 3);

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


		// Tuning mode displays the result of the threshold
		if (SettingsActivity.tuningMode()) {
			// Normalize and scale binary mask so it can be displayed
			Core.normalize(mask, mask, 0, 255, Core.NORM_MINMAX, input.type(), new Mat());
			Core.convertScaleAbs(mask, mask);
			return mask;
		}

		// Identify all contours
		ArrayList<MatOfPoint> contours = new ArrayList<>();
		Imgproc.findContours(mask, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
		if (contours.isEmpty()) return input;
		// Since multiple contours may be detected we need to single out the best one

		MatOfPoint contour = bestContour(contours, input);

		if (contour != null) {
			// Track corners of target
			Point[] corners = getCorners(contour);

			// Draw corners on image
			for (int i = 0; i < corners.length; i++) {
				Imgproc.circle(input, corners[i], 5, new Scalar((corners.length > 1) ? 255/(corners.length-1) * i : 0, 0, 0), -1);
			}

			double ratio = (2 * mPPI)/Math.max(corners[1].x - corners[0].x, corners[3].x - corners[2].x);

			double target = (trackingLeft) ? corners[0].x + (Constants.kVisionTargetWidth/2) * mPPI / ratio
																	: corners[1].x - (Constants.kVisionTargetWidth/2) * mPPI / ratio;

			Imgproc.circle(input, new Point(target, mHeight/2), 5, new Scalar(0, 0, 255), -1);

//        Log.d(TAG, "" + (target - corners[0].x)/mPPI);

			xDist = (target - mWidth/2)/mPPI * ratio;

			Imgproc.putText(input,
					String.format(Locale.getDefault(), "%.2f",
					xDist), new Point(0, mHeight - 30),
					Core.FONT_HERSHEY_SIMPLEX, 3/mResolutionFactor, new Scalar(0, 255, 0), 3);
		} else {
			xDist = null;
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
//        double scalar = mPPI, width = Constants.kTapeWidth * scalar, height = Constants.kVisionTargetHeight * scalar, depth = Constants.kPegLength * scalar;
//        double leftX = (mWidth - width) / 2, topY = (mHeight - height) / 2, rightX = leftX + width, bottomY = topY + height, z = 0 - 2 * scalar;
		double scalar = mPPI, width = Constants.kVisionTargetWidth, height = Constants.kVisionTargetHeight,
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
//		Calib3d.solvePnPRansac(srcPoints, dstPoints, intrinsicMatrix, distCoeffs, rvecs, tvecs, false, 100, 8.0f, 0.99, new Mat(), Calib3d.SOLVEPNP_P3P);

		// Convert the z translation to inches and subtract by the peg length to get distance from the peg tip
//		double zDist = (tvecs.get(2, 0)[0] + Constants.kGalaxyFocalLengthZ) / 2231 - Constants.kPegLength;
//		double zDist = (tvecs.get(2,0)[0] + Constants.kNexusFocalOffsetZ) * Constants.kNexusFocalScaleZ - Constants.kPegLength;
		double zDist = (tvecs.get(2, 0)[0]) * conv;

        /*
         * What x position is the peg supposed to be at? This depends on which
         * target is being tracked - right or left.
         */

        double pegBaseX = 0, pegTipX = 0, pegPos = -width/2;
        if (trackingLeft) {
//            pegBaseX = leftX + width/2;
			pegTipX = pegBaseX + tapeWidth/2/zDist;
//			pegPos = width/2;
//			pegX = leftX;
        } else {
//            pegBaseX = rightX - width/2;
			pegTipX = pegBaseX - tapeWidth/2/zDist;
//			pegPos = -width/2;
//			pegX = rightX;
        }

		// Given the perceived x displacement,
//        xDist = zDist;
//        xDist = ((arr[1].x - mWidth / 2) * zDist / 4) / mPPI;
		double xShift = (tvecs.get(0,0)[0]) * conv;
		double y = ((lastZ*lastX_1) - (lastZ_1*lastX))/(lastX-lastX_1), l = 61.5;
		y = 3;

		xDist = pegPos + l*xShift/(y+zDist);
//		xDist = xShift;
		xDist += SettingsActivity.getNexusShift();
		double[] angles = rvecs.toArray();
		turnAngle = Math.toDegrees(angles[1]);

		lastZ_1 = lastZ; lastX_1 = lastX;
		lastZ = zDist; lastX = xShift;

        MatOfPoint3f newPoints = new MatOfPoint3f(new Point3(pegBaseX, (topY+bottomY)/2, 0),
                                                new Point3(pegTipX, (topY+bottomY)/2, 1 * depth)
												,new Point3(0, 0, 0),
												new Point3(-xDist, tvecs.get(1,0)[0]*conv, tvecs.get(2,0)[0]*conv)
		);

        MatOfPoint2f result = new MatOfPoint2f();
        Calib3d.projectPoints(newPoints, rvecs, tvecs, intrinsicMatrix, distCoeffs, result);
        Point[] arr = result.toArray();

        // Estimates the position of the base and tip of the peg
        Imgproc.line(input, arr[0], arr[1], new Scalar(255, 255, 255), 5);
		Imgproc.line(input, arr[2], arr[3], new Scalar(0, 0, 255), 5);

		for(Point p: arr){
			Imgproc.circle(input, p, 7, new Scalar(0,255,0));
		}
//		Imgproc.circle(input, arr[2], 12, new Scalar(0,0,255));
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
		trackingLeft = SettingsActivity.trackingLeftTarget();
		double threshold = 0.2 * Imgproc.contourArea(contours.get(0));

		List<MatOfPoint> found = new ArrayList<MatOfPoint>();
		for (MatOfPoint contour: contours) {
			if (Imgproc.contourArea(contour) < threshold) found.add(contour);
		}
		contours.removeAll(found);
		Imgproc.drawContours(input, found, -1, new Scalar(0, 0, 255));

		// Were both strips of tape detected?
		if (contours.size() == 2) {
			Imgproc.drawContours(input, contours, 0, new Scalar(255, 0, 0));
			Imgproc.drawContours(input, contours, 1, new Scalar(0, 255, 0));
			// Calculate bounding rectangles of contours to compare x position
			Rect oneRect = Imgproc.boundingRect(contours.get(0)), twoRect = Imgproc.boundingRect(contours.get(1));
			double oneArea = oneRect.area(), twoArea = twoRect.area();
			double ratioTolerance = 0.5;
			/**
			 * If one contour area is a certain ratio of the other, then we can
			 * assume that it's being obscured and track the other one.
			 * If we choose to track the other target, we must determine if it's
			 * on the left or right
			 */

//			if (Math.min(oneArea, twoArea) / Math.max(oneArea, twoArea) < ratioTolerance) {
//				if (oneArea <= twoArea) {
//					trackingLeft = oneRect.x >= twoRect.x;
//					contours.remove(0);
//				} else {
//					trackingLeft = oneRect.x < twoRect.x;
//				}
//				// If the areas don't meet this ratio, stick to the target the app was told to track
//			} else if (oneRect.x < twoRect.x != trackingLeft) contours.remove(0);

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

	/*private Point centroid(Point[] corners) {
		Point[] tCentroids = new Point[4];
		int count = 0;
		for (int i = 0; i < 2; i++) {
			for (int j = i+1; j < 3; j++) {
				for (int k = j+1; k < 4; k++) {
					tCentroids[count++] = new Point((corners[i].x + corners[j].x + corners[k].x)/3,
													(corners[i].y + corners[j].y + corners[k].y)/3);
				}
			}
		}
		double[] vector1 = {tCentroids[3].x - tCentroids[0].x, tCentroids[3].y - tCentroids[0].y},
				vector2 = {tCentroids[2].x - tCentroids[1].x, tCentroids[2].y - tCentroids[1].y},
				vector3 = {tCentroids[2].x - tCentroids[0].x, tCentroids[2].y - tCentroids[0].y};

		double cross = vector1[0] * vector2[1] - vector1[1] * vector2[0];
		if (Math.abs(cross) < Math.pow(10, -8)) return null;
		double scalar = (vector3[0] * vector2[1] - vector3[1] * vector2[0])/cross;
		return new Point(tCentroids[0].x + vector1[0] * scalar, tCentroids[0].y + vector1[1] * scalar);
	}*/


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
	public static Double getXDisplacement() { return xDist; }
	public static long getCycleTime() { return cycleTime; }

	public static void toggleFlash(boolean flashlightOn) {
		SettingsActivity.setFlashlightOn(flashlightOn);
		mCameraView.toggleFlashLight(flashlightOn);
	}
}
