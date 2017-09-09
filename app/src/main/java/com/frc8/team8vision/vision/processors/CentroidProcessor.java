package com.frc8.team8vision.vision.processors;

import com.frc8.team8vision.Constants;
import com.frc8.team8vision.SettingsActivity;
import com.frc8.team8vision.vision.CameraInfo;
import com.frc8.team8vision.vision.VisionProcessorBase;
import com.frc8.team8vision.vision.VisionData;

import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

import static com.frc8.team8vision.vision.VisionUtil.*;

/**
 * Created by Alvin on 9/8/2017.
 */

public class CentroidProcessor extends VisionProcessorBase {

	@Override
	public VisionData[] process(Mat input, Mat mask) {
		// Find the peg target contour
		ArrayList<MatOfPoint> contours = new ArrayList<>();
		Imgproc.findContours(mask, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
		if (contours.isEmpty()) {
			output_data[IDX_OUT_XDIST].setToDefault();
			output_data[IDX_OUT_ZDIST].setToDefault();
			output_data[IDX_OUT_FUNCTION_EXECUTION_CODE].set(EXECUTION_CODE_FAIL);
			output_data[IDX_OUT_EXECUTION_MESSAGE].set("Empty set of contours");
			return output_data;
		}
		MatOfPoint contour = bestContour(contours, input);

		if (contour != null) {
			Point[] corners = getCorners(contour);
			output_data[IDX_OUT_ZDIST].set(getPosePnP(corners, input)[2]);

			// Draw corners on image
			for (int i = 0; i < corners.length; i++) {
				Imgproc.circle(input, corners[i], 5, new Scalar((corners.length > 1) ? 255/(corners.length-1) * i : 0, 0, 0), -1);
			}

			double ratio = Math.max(corners[1].x - corners[0].x, corners[3].x - corners[2].x)/2;
			double target = (CameraInfo.isTrackingLeft()) ? corners[0].x + (Constants.kVisionTargetWidth/2) * ratio
					: corners[1].x - (Constants.kVisionTargetWidth/2) * ratio;

			Imgproc.circle(input, new Point(target, CameraInfo.Height()/2), 5, new Scalar(0, 0, 255), -1);
			output_data[IDX_OUT_XDIST].set((target - CameraInfo.Width()/2) / ratio + SettingsActivity.getNexusShift());

		} else {
			output_data[IDX_OUT_XDIST].setToDefault();
			output_data[IDX_OUT_ZDIST].setToDefault();
		}

		return output_data;
	}
}
