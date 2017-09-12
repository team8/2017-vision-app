package com.frc8.team8vision.vision;

import com.frc8.team8vision.util.DataExistsCallback;

import org.opencv.core.Mat;

/**
 * Created by Alvin on 9/8/2017.
 */

public abstract class VisionProcessorBase {

	public static final int EXCECUTION_CODE_OKAY = 0;
	public static final int EXECUTION_CODE_FAIL = 1;

	public static final int OUT_DIM = 4;
	public static final int IDX_OUT_FUNCTION_EXECUTION_CODE = 0;
	public static final int IDX_OUT_EXECUTION_MESSAGE = 1;
	public static final int IDX_OUT_XDIST = 2;
	public static final int IDX_OUT_ZDIST = 3;

	protected VisionData[] output_data;

	public VisionProcessorBase(){
			output_data = new VisionData[OUT_DIM];

			output_data[IDX_OUT_FUNCTION_EXECUTION_CODE] = new VisionData<Integer>(0, 1, new DataExistsCallback<Integer>(){});
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
