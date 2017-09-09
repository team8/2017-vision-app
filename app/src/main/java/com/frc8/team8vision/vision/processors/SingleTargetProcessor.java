package com.frc8.team8vision.vision.processors;

import com.frc8.team8vision.SettingsActivity;
import com.frc8.team8vision.vision.VisionProcessorBase;
import com.frc8.team8vision.vision.VisionData;

import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

import static com.frc8.team8vision.vision.VisionUtil.*;

/**
 * Created by Alvin on 9/8/2017.
 */

public class SingleTargetProcessor extends VisionProcessorBase {

	@Override
	public VisionData[] process(Mat input, Mat mask) {
		// Find the peg target contour
		ArrayList<MatOfPoint> contours = new ArrayList<>();
		Imgproc.findContours(mask, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
		if (contours.isEmpty()) {
			output_data[IDX_OUT_FUNCTION_EXECUTION_CODE].set(EXECUTION_CODE_FAIL);
			output_data[IDX_OUT_EXECUTION_MESSAGE].set("Empty set of contours");
			return output_data;
		}
		MatOfPoint contour = bestContour(contours, input);

		if (contour != null) {
			Point[] corners = getCorners(contour);
			double[] tvecs = getPosePnP(corners, input);
			output_data[IDX_OUT_ZDIST].set(tvecs[2]);
			output_data[IDX_OUT_XDIST].set(tvecs[0] + SettingsActivity.getNexusShift());

		} else {
			output_data[IDX_OUT_XDIST].setToDefault();
			output_data[IDX_OUT_ZDIST].setToDefault();
		}

		return output_data;
	}
}
