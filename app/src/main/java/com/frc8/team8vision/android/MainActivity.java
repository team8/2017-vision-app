package com.frc8.team8vision.android;

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

import com.frc8.team8vision.util.Constants.Constants;
import com.frc8.team8vision.R;
import com.frc8.team8vision.util.VisionPreferences;
import com.frc8.team8vision.vision.DataTransferModeSelector;
import com.frc8.team8vision.vision.VisionInfoData;
import com.frc8.team8vision.vision.VisionProcessorBase;
import com.frc8.team8vision.vision.ProcessorSelector;
import com.frc8.team8vision.vision.ProcessorSelector.ProcessorType;
import com.frc8.team8vision.vision.VisionDataUnit;

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
 *
 * @author Calvin Yan
 */
public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

	private static final String TAG = Constants.kTAG+"MainActivity";

	private ProcessorSelector visionProcessor;
	private DataTransferModeSelector.VisionDataTransferModeSelector visionDataTransferModeSelector;
	private DataTransferModeSelector.VideoDataTransferModeSelector videoTransferModeSelector;

	private static SketchyCameraView mCameraView;
	private boolean isSettingsPaused = false;

	private long lastCycleTimestamp = 0;

	private int mWidth = 0, mHeight = 0;
	private int mResolutionFactor = 3;      // Divides screen images by given factor

	private boolean opencvLoaded = false;

	/**
	 * The delay between starting the app and loading OpenCV libraries means that
	 * all OpenCV objects must be instantiated in this callback function.
	 */
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {

			onAllLoaded();

			Mat intrinsicMatrix = null;
			MatOfDouble distCoeffs = null;

			switch (status) {
				case LoaderCallbackInterface.SUCCESS: {

					opencvLoaded = true;

					VisionPreferences.updateSettings();
					visionProcessor.setProcessor(VisionPreferences.getProcessorType());

					Log.i(TAG, "OpenCV load success");

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
					} else {
						// Nexus 5x is being used
						for (int i = 0; i < 3; i++) {
							for (int j = 0; j < 3; j++) {
								intrinsicMatrix.put(i, j, Constants.kNexusIntrinsicMatrix[i][j]);
							}
						}
						distCoeffs.fromArray(Constants.kNexusDistortionCoefficients);
					}
				} break;
				default: {
					super.onManagerConnected(status);
				} break;
			}

			CameraInfo.setIntrinsics(intrinsicMatrix);
			CameraInfo.setDistortion(distCoeffs);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		// Load the OpenCV library
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
	}

	private void onAllLoaded() {

		mCameraView = new SketchyCameraView(this, -1);
		setContentView(mCameraView);
		mCameraView.setCvCameraViewListener(this);
		mCameraView.setMaxFrameSize(1920 / mResolutionFactor, 1080 / mResolutionFactor);

		visionProcessor = new ProcessorSelector();
		visionProcessor.setProcessor(ProcessorType.CENTROID);

		visionDataTransferModeSelector = new DataTransferModeSelector.VisionDataTransferModeSelector(this, false);
		visionDataTransferModeSelector.setTransfererMode(DataTransferModeSelector.DataTransferMode.CAT_JSON);

		videoTransferModeSelector = new DataTransferModeSelector.VideoDataTransferModeSelector(this, false);
		videoTransferModeSelector.setTransfererMode(DataTransferModeSelector.DataTransferMode.SOCKET);

		VisionPreferences.initialize(this);
	}

	@Override
	public void onPause() {

		visionDataTransferModeSelector.getTransferer().pause();
		videoTransferModeSelector.getTransferer().pause();

		super.onPause();

		if (mCameraView != null)
			mCameraView.disableView();
	}

	@Override
	public void onResume() {

		super.onResume();
		isSettingsPaused = false;

		if (opencvLoaded) {

			mCameraView.enableView();

			VisionPreferences.updateSettings();
			visionProcessor.setProcessor(VisionPreferences.getProcessorType());

			visionDataTransferModeSelector.getTransferer().resume();
			videoTransferModeSelector.getTransferer().resume();
		}
	}

	@Override
	public void onDestroy() {

		super.onDestroy();

		visionDataTransferModeSelector.stopAll();
		videoTransferModeSelector.stopAll();

		if (mCameraView != null) {
			mCameraView.disableView();
		}
	}

	@Override
	public void onCameraViewStarted(int width, int height) {

		mWidth = width;
		mHeight = height;

		CameraInfo.setDims(height, width);


		// Reduce exposure and turn on flashlight - to be used with reflective tape
		mCameraView.setParameters();
		mCameraView.toggleFlashLight(VisionPreferences.isFlashlightOn());

		if (!this.isFocusLocked() || !isSettingsPaused) {
			visionDataTransferModeSelector.getTransferer().resume();
			videoTransferModeSelector.getTransferer().resume();
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
		Mat imageRGB = inputFrame.rgba();
		if (!isGalaxy()) Core.flip(imageRGB, imageRGB, -1); // Necessary because Nexus camera feed is inverted
		imageRGB = track(imageRGB);
		Mat imageToPass = imageRGB.clone();
		if(imageRGB.channels() == 4){
			Imgproc.cvtColor(imageRGB, imageToPass, Imgproc.COLOR_BGRA2RGBA);
		}

		VisionInfoData.setFrame(imageToPass);

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
		if (lastCycleTimestamp != 0) {
			CameraInfo.updateCycleTime(System.currentTimeMillis() - lastCycleTimestamp);
		}
		lastCycleTimestamp = System.currentTimeMillis();

		Mat mask = new Mat();
		Mat imageHSV = new Mat();

		int[] sliderValues = VisionPreferences.getSliderValues();

		// Create mask from hsv threshold
		Scalar lower_bound = new Scalar(sliderValues[0], sliderValues[1], sliderValues[2]),
				upper_bound = new Scalar(sliderValues[3], sliderValues[4], sliderValues[5]);
		Imgproc.cvtColor(input, imageHSV, Imgproc.COLOR_RGB2HSV);
		Core.inRange(imageHSV, lower_bound, upper_bound, mask);

		// Tuning mode displays the result of the threshold
		if (VisionPreferences.isTuningMode()) {
			Core.normalize(mask, mask, 0, 255, Core.NORM_MINMAX, input.type(), new Mat());
			Core.convertScaleAbs(mask, mask);
			return mask;
		}

		VisionDataUnit[] out_data = visionProcessor.getProcessor().process(input, mask);
		if((Integer)out_data[VisionProcessorBase.IDX_OUT_FUNCTION_EXECUTION_CODE].get()
			!= VisionProcessorBase.EXECUTION_CODE_OKAY){
			Log.e(TAG, "track Error:\n\t" +
					out_data[VisionProcessorBase.IDX_OUT_EXECUTION_MESSAGE].get());
		}

		VisionDataUnit<Double> xDist = out_data[VisionProcessorBase.IDX_OUT_XDIST];
		VisionDataUnit<Double> zDist = out_data[VisionProcessorBase.IDX_OUT_ZDIST];
		VisionDataUnit<Double> yDist = out_data[VisionProcessorBase.IDX_OUT_YDIST];

		VisionInfoData.setXDist(xDist);
		VisionInfoData.setZDist(zDist);
		VisionInfoData.setYDist(yDist);

		String printval = "<" +
				(xDist != null ? String.format(Locale.getDefault(), "%.2f", xDist.get()) : "NaN") + ", " +
				(zDist != null ? String.format(Locale.getDefault(), "%.2f", zDist.get()) : "NaN") + ">";
		Imgproc.putText(input, printval, new Point(0, mHeight - 30),
				Core.FONT_HERSHEY_SIMPLEX, 2.5 / mResolutionFactor, new Scalar(0, 255, 0), 3);
		Imgproc.putText(input, Double.toString(1000/CameraInfo.getCycleTime()),
				new Point(mWidth - 200 / mResolutionFactor, mHeight - 30),
				Core.FONT_HERSHEY_SIMPLEX, 2.5 / mResolutionFactor, new Scalar(0, 255, 0), 3);
        return input;
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
	public boolean isFocusLocked(){
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		int lockValue = preferences.getInt("Focus Lock Value", 0);
		return lockValue >= 80;
	}
}
