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

import com.frc8.team8vision.networking.JPEGStreamerThread;
import com.frc8.team8vision.networking.JSONVisionDataThread;
import com.frc8.team8vision.vision.AbstractVisionProcessor;
import com.frc8.team8vision.vision.ProcessorSelector;
import com.frc8.team8vision.vision.VisionData;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.Locale;

/**
 * The app's startup activity, as suggested by its name. Handles all
 * camera operations and vision processing.
 * @author Calvin Yan
 */
public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

	private static final String TAG = Constants.kTAG+"MainActivity";

	private static Mat imageRGB;
	private static Mat imageRGB_raw;

	private static VisionData<Double> xDist = null, zDist = null;
	private static long cycleTime = 0;

	private ProcessorSelector visionProcessor;

	private MatOfDouble distCoeffs;
	private Mat intrinsicMatrix, imageHSV;

	private static SketchyCameraView mCameraView;
	private static boolean isSettingsPaused = false;

	private long lastCycleTimestamp = 0;

	private int mWidth = 0, mHeight = 0;
	private int[] sliderValues = new int[6];
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
//						mPPI = Constants.kGalaxyPixelsPerInch;
					} else {
						// Nexus 5x is being used
						for (int i = 0; i < 3; i++) {
							for (int j = 0; j < 3; j++) {
								intrinsicMatrix.put(i, j, Constants.kNexusIntrinsicMatrix[i][j]);
							}
						}
						distCoeffs.fromArray(Constants.kNexusDistortionCoefficients);
//						mPPI = Constants.kNexusPixelsPerInch;
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

		visionProcessor = new ProcessorSelector(mHeight, mWidth, intrinsicMatrix, distCoeffs, SettingsActivity.trackingLeftTarget());
		visionProcessor.setProcessor(ProcessorSelector.ProcessorType.CENTROID);

		JSONVisionDataThread.getInstance().start(this, JSONVisionDataThread.WriteState.JSON);
		JPEGStreamerThread.getInstance().start(this);
	}

	@Override
	public void onPause() {
		JPEGStreamerThread.getInstance().pause();
		JSONVisionDataThread.getInstance().pause();
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
		JSONVisionDataThread.getInstance().resume();
		JPEGStreamerThread.getInstance().resume();
	}

	public void onDestroy() {
		JSONVisionDataThread.getInstance().destroy();
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
			JSONVisionDataThread.getInstance().resume();
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

		VisionData[] out_data = visionProcessor.getProcessor().process(input, mask);
		if((Integer)out_data[AbstractVisionProcessor.IDX_OUT_FUNCTION_EXECUTION_CODE].get()
			== AbstractVisionProcessor.EXCECUTION_CODE_OKAY){
			Log.e(TAG, "track Error:\n\t" +
					(String)out_data[AbstractVisionProcessor.IDX_OUT_EXECUTION_MESSAGE].get());
		}

		xDist = out_data[AbstractVisionProcessor.IDX_OUT_XDIST];
		zDist = out_data[AbstractVisionProcessor.IDX_OUT_ZDIST];

		String printval = "<" +
				(xDist != null ? String.format(Locale.getDefault(), "%.2f", xDist) : "NaN") + ", " +
				(zDist != null ? String.format(Locale.getDefault(), "%.2f", zDist) : "NaN") + ">";
		Imgproc.putText(input, printval, new Point(0, mHeight - 30),
				Core.FONT_HERSHEY_SIMPLEX, 2.5 / mResolutionFactor, new Scalar(0, 255, 0), 3);
		Imgproc.putText(input, Double.toString(cycleTime),
				new Point(mWidth - 200 / mResolutionFactor, mHeight - 30),
				Core.FONT_HERSHEY_SIMPLEX, 2.5 / mResolutionFactor, new Scalar(0, 255, 0), 3);
        return input;
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
	public static Double getXDisplacement() {
		return (Double) xDist.get();
	}
	public static Double getZDisplacement() {
		return (Double) zDist.get();
	}
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
