package com.frc8.team8vision.vision.processors;

import android.util.Log;

import com.frc8.team8vision.util.AreaComparator;
import com.frc8.team8vision.util.Constants;
import com.frc8.team8vision.util.VisionPreferences;
import com.frc8.team8vision.android.CameraInfo;
import com.frc8.team8vision.util.VisionUtil;
import com.frc8.team8vision.vision.VisionProcessorBase;
import com.frc8.team8vision.vision.VisionDataUnit;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;

import static com.frc8.team8vision.util.VisionUtil.*;

public class CentroidProcessor extends VisionProcessorBase {

	private final MatOfPoint3f kLeftTargetMatrix, kRightTargetMatrix;

	public CentroidProcessor() {
		kLeftTargetMatrix  = new MatOfPoint3f(Constants.kLeftSourcePoints );
		kRightTargetMatrix = new MatOfPoint3f(Constants.kRightSourcePoints);
	}

	@Override
	public MatOfPoint[] getBestContours(ArrayList<MatOfPoint> contours, Mat input) {

		final boolean dynamicTracking = VisionPreferences.isDynamicTracking();

		if (contours.size() >= 2) {

			// Sort contours in decreasing order of area
			Collections.sort(contours, new AreaComparator());

			// Check if the first (biggest) contour is the left one
			final boolean firstIsLeft = contours.get(0).toArray()[0].x < contours.get(1).toArray()[0].x;
			// Find left and right contours
			MatOfPoint
				left  = firstIsLeft ? contours.get(0) : contours.get(1),
				right = firstIsLeft ? contours.get(1) : contours.get(0);

			final boolean leftIsBigger = Imgproc.contourArea(left) > Imgproc.contourArea(right);
			if (dynamicTracking)
				VisionPreferences.setTrackingLeft(leftIsBigger);

			// Find the final contour based on which target we are aiming for
			final MatOfPoint finalContour = VisionPreferences.isTrackingLeft() ? left : right;

			// Draw tape contours on screen
			Imgproc.drawContours(input, contours, 0, new Scalar(255, 0, 0));
			Imgproc.drawContours(input, contours, 1, new Scalar(0, 255, 0));

			// Return first two contours which should be the biggest
			return new MatOfPoint[] { finalContour };
		}
		return null;
	}

	@Override
	public VisionDataUnit[] processContours(MatOfPoint[] bestContours, Mat input) {

		final boolean trackingLeft = VisionPreferences.isTrackingLeft();

		if (bestContours != null && bestContours.length == 1) {

			final Point[] corners = VisionUtil.getCorners(bestContours[0], 0);

			final Point3 posePnP = getPosePnP(trackingLeft ? kLeftTargetMatrix : kRightTargetMatrix, corners, input);
			output_data[IDX_OUT_ZDIST].set(posePnP.z - VisionPreferences.getZ_shift());

			// Draw corners on image
			for (int i = 0; i < corners.length; i++)
				Imgproc.circle(input, corners[i], 5, new Scalar((corners.length > 1) ? 255/(corners.length-1) * i : 0, 0, 0), -1);

			final double
				ratio = Math.max(corners[1].x - corners[0].x, corners[3].x - corners[2].x)/2,
				target = trackingLeft
					? corners[0].x + (Constants.kVisionTargetWidth/2) * ratio
					: corners[1].x - (Constants.kVisionTargetWidth/2) * ratio,
				hh = CameraInfo.Height()/2.0, hw = CameraInfo.Width()/2.0;

			Imgproc.circle(input, new Point(target, hh), 5, new Scalar(0, 0, 255), -1);
			output_data[IDX_OUT_XDIST].set((target - hw) / ratio + VisionPreferences.getX_shift());

		} else {
			output_data[IDX_OUT_XDIST].setToDefault();
			output_data[IDX_OUT_ZDIST].setToDefault();
		}

		return output_data;
	}
}
