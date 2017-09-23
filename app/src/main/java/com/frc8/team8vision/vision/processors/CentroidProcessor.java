package com.frc8.team8vision.vision.processors;

import com.frc8.team8vision.util.AreaComparator;
import com.frc8.team8vision.util.Constants;
import com.frc8.team8vision.util.VisionPreferences;
import com.frc8.team8vision.android.CameraInfo;
import com.frc8.team8vision.vision.VisionProcessorBase;
import com.frc8.team8vision.vision.VisionData;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;

import static com.frc8.team8vision.util.VisionUtil.*;

public class CentroidProcessor extends VisionProcessorBase {

	@Override
	public MatOfPoint[] getBestContours(ArrayList<MatOfPoint> contours, Mat input) {

		if (contours.size() >= 1) {
			// Sort contours in decreasing order of area
			Collections.sort(contours, new AreaComparator());
			// Get which contours is first
			final boolean firstIsLeft = contours.get(0).toArray()[0].x < contours.get(1).toArray()[0].x;
			MatOfPoint finalContour = firstIsLeft ? contours.get(0) : contours.get(1);
			// Draw tape contours on screen
			Imgproc.drawContours(input, contours, 0, new Scalar(255, 0, 0));
			Imgproc.drawContours(input, contours, 1, new Scalar(0, 255, 0));
			// Return first two contours which should be the biggest
			return new MatOfPoint[] { finalContour };
		} else {
			return null;
		}
	}

	@Override
	public VisionData[] processContours(MatOfPoint[] matCorners, Mat input) {

		final boolean trackingLeft = VisionPreferences.isTrackingLeft();

		final Point[] corners = matCorners[0].toArray();

		output_data[IDX_OUT_ZDIST].set(getPosePnP(trackingLeft ? Constants.kLeftSourcePoints : Constants.kRightSourcePoints, corners, input).z + VisionPreferences.getZ_shift());

		// Draw corners on image
		for (int i = 0; i < corners.length; i++) {
			Imgproc.circle(input, corners[i], 5, new Scalar((corners.length > 1) ? 255/(corners.length-1) * i : 0, 0, 0), -1);
		}

		double ratio = Math.max(corners[1].x - corners[0].x, corners[3].x - corners[2].x)/2;
		double target = (VisionPreferences.isTrackingLeft()) ? corners[0].x + (Constants.kVisionTargetWidth/2) * ratio
				: corners[1].x - (Constants.kVisionTargetWidth/2) * ratio;

		Imgproc.circle(input, new Point(target, CameraInfo.Height()/2), 5, new Scalar(0, 0, 255), -1);
		output_data[IDX_OUT_XDIST].set((target - CameraInfo.Width()/2) / ratio + VisionPreferences.getX_shift());

		return output_data;
	}
}
