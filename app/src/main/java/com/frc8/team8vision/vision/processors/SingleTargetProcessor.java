package com.frc8.team8vision.vision.processors;

import com.frc8.team8vision.android.CameraInfo;
import com.frc8.team8vision.processing.KalmanFilter;
import com.frc8.team8vision.util.Constants.KalmanGains;
import com.frc8.team8vision.util.VisionPreferences;
import com.frc8.team8vision.util.AreaComparator;
import com.frc8.team8vision.util.Constants.Constants;
import com.frc8.team8vision.util.VisionUtil;
import com.frc8.team8vision.vision.VisionProcessorBase;
import com.frc8.team8vision.vision.VisionDataUnit;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;

public class SingleTargetProcessor extends VisionProcessorBase {

	private final MatOfPoint3f kLeftTargetMatrix, kRightTargetMatrix;
	private final int kXPointShift;

	private KalmanFilter mKalmanFilter;

	public SingleTargetProcessor() {
		kLeftTargetMatrix = new MatOfPoint3f(Constants.kLeftSourcePoints);
		kRightTargetMatrix = new MatOfPoint3f(Constants.kRightSourcePoints);
		kXPointShift = CameraInfo.Width()/2;

		mKalmanFilter = new KalmanFilter(new float[]{0.5f, 0.5f, 0.5f}, KalmanGains.kMeasurementNoise, KalmanGains.kProcessNoise);
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

		} else {
			return null;
		}
	}

	@Override
	public VisionDataUnit[] processContours(MatOfPoint[] bestContours, Mat input) {

		if (bestContours != null && bestContours.length == 1) {

			final boolean isTrackingLeft = VisionPreferences.isTrackingLeft();

			// Get corners for both targets
			final Point[] corners = VisionUtil.getCorners(bestContours[0], kXPointShift);

			final Point3 posePnP = VisionUtil.getPosePnP(isTrackingLeft ? kLeftTargetMatrix : kRightTargetMatrix, corners, input);

			Mat pnpMat = new Mat(new Size(3,1), CvType.CV_32F);
			pnpMat.put(0,0,posePnP.x);
			pnpMat.put(1,0,posePnP.y);
			pnpMat.put(2,0,posePnP.z);

			mKalmanFilter.update(pnpMat);
			Point3 filteredPose = mKalmanFilter.getState();

			output_data[IDX_OUT_ZDIST].set(filteredPose.z + VisionPreferences.getZ_shift());
			output_data[IDX_OUT_XDIST].set(filteredPose.x + VisionPreferences.getX_shift());
		} else {
			output_data[IDX_OUT_XDIST].setToDefault();
			output_data[IDX_OUT_ZDIST].setToDefault();
		}
		return output_data;
	}
}
