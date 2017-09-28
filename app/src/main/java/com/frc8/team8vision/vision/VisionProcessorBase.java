package com.frc8.team8vision.vision;

import com.frc8.team8vision.util.DataExistsCallback;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

/**
 * Base class for all vision processors.
 */
public abstract class VisionProcessorBase {

	/**
	 * Checks if a double exists for the data structure.
	 */
	private class DoubleExistsCallback extends DataExistsCallback<Double> {
		@Override
		public boolean doesExist(Double data) {
			return !(data == null || data.isInfinite() || data.isNaN());
		}
	}

	public static final int
		EXECUTION_CODE_OKAY = 0,
		EXECUTION_CODE_FAIL = 1;

	public static final int
		OUT_DIM = 4,
		IDX_OUT_FUNCTION_EXECUTION_CODE = 0,
		IDX_OUT_EXECUTION_MESSAGE = 1,
		IDX_OUT_XDIST = 2,
		IDX_OUT_ZDIST = 3;

	protected VisionData[] output_data;

	public VisionProcessorBase() {

		output_data = new VisionData[OUT_DIM];

		output_data[IDX_OUT_FUNCTION_EXECUTION_CODE] = new VisionData<>(0, 1, new DataExistsCallback<Integer>(){});
		output_data[IDX_OUT_EXECUTION_MESSAGE] = new VisionData<>("Safe execution", null, new DataExistsCallback<String>(){});
		output_data[IDX_OUT_XDIST] = new VisionData<>(Double.NaN, Double.NaN, new DoubleExistsCallback());
		output_data[IDX_OUT_ZDIST] = new VisionData<>(Double.NaN, Double.NaN, new DoubleExistsCallback());
	}

	/**
	 * Process the image received from the camera.
	 *
	 * @param input The raw image input from the camera.
	 * @param mask A filtered image of ones and zeros.
	 * @return Vision data.
	 */
	public VisionData[] process(Mat input, Mat mask) {

		// Find contours that represent tape on the peg
		ArrayList<MatOfPoint> contours = new ArrayList<>();
		Imgproc.findContours(mask, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

		MatOfPoint[] bestContours = getBestContours(contours, input);

		return processContours(bestContours, input);
	}

	/**
	 * Processes the best contours from {@link #getBestContours(ArrayList, Mat)}.
	 * These are contours that represent the reflective tape.
	 *
	 * @param corners The corners of the tape(s) in image space.
	 * @param input The raw video image from the camera.
	 * @return Vision data.
	 */
	public abstract VisionData[] processContours(MatOfPoint[] corners, Mat input);

	/**
	 * Get best contours to process from a list.
	 *
	 * @param contours Contours found from the masked image.
	 * @param input The raw image data from the camera.
	 * @return Array of contours which represents best points.
	 */
	public abstract MatOfPoint[] getBestContours(ArrayList<MatOfPoint> contours, Mat input);
}
