package com.frc8.team8vision.vision;

import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;

/**
 * Created by Alvin on 9/8/2017.
 */

public abstract class AbstractVisionProcessor {

	public static final int EXCECUTION_CODE_OKAY = 0;
	public static final int EXECUTION_CODE_FAIL = 1;

	public static final int OUT_DIM = 4;
	public static final int IDX_OUT_FUNCTION_EXECUTION_CODE = 0;
	public static final int IDX_OUT_EXECUTION_MESSAGE = 1;
	public static final int IDX_OUT_XDIST = 2;
	public static final int IDX_OUT_ZDIST = 3;

	protected VisionData[] output_data;

	protected int mHeight, mWidth;
	protected Boolean trackingLeft;

	protected Mat intrinsicMatrix;
	protected MatOfDouble distCoeffs;

	public AbstractVisionProcessor(int height, int width, Mat intrinsics, MatOfDouble distortion, boolean isTrackingLeft){
			mHeight = height;
			mWidth = width;
			intrinsicMatrix = intrinsics;
			distCoeffs = distortion;
			trackingLeft = isTrackingLeft;

			output_data = new VisionData[OUT_DIM];

			output_data[IDX_OUT_FUNCTION_EXECUTION_CODE] = new VisionData<Integer>(0, null, new DataExistsCallback<Integer>(){});
			output_data[IDX_OUT_EXECUTION_MESSAGE] = new VisionData<String>("Safe execution", null, new DataExistsCallback<String>(){});
			output_data[IDX_OUT_XDIST] = new VisionData<Double>(Double.NaN, Double.NaN, new DataExistsCallback<Double>() {
				@Override
				public boolean doesExist(Double data) {
					return !(data == null || data.isInfinite() || data.isNaN());
				}
			});
			output_data[IDX_OUT_ZDIST] = new VisionData<Double>(Double.NaN, Double.NaN, new DataExistsCallback<Double>() {
				@Override
				public boolean doesExist(Double data) {
					return !(data == null || data.isInfinite() || data.isNaN());
				}
			});
	}

	public abstract VisionData[] process(Mat input, Mat mask);
}
