package com.frc8.team8vision.util;

import com.frc8.team8vision.android.CameraInfo;
import com.frc8.team8vision.util.Constants.Constants;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;

/**
 * Contains utility functions for vision
 */
public abstract class VisionUtil {


//	/**
//	 * Remove all contours that are below a certain area threshold. Used to remove salt noise.
//	 */
//	public static MatOfPoint bestContour(ArrayList<MatOfPoint> contours, Mat input) {
//		boolean trackingLeft = VisionPreferences.isTrackingLeft();
//
//		// Sort contours in decreasing order of area
//		Collections.sort(contours, new Comparator<MatOfPoint>() {
//			public int compare(MatOfPoint one, MatOfPoint two) {
//				return Double.compare(Imgproc.contourArea(two), Imgproc.contourArea(one));
//			}
//		});
//		double threshold = 0.4 * Imgproc.contourArea(contours.get(0));
//
//		List<MatOfPoint> found = new ArrayList<>();
//		for (MatOfPoint contour: contours) {
//			if (Imgproc.contourArea(contour) < threshold) found.add(contour);
//		}
//		contours.removeAll(found);
//		Imgproc.drawContours(input, found, -1, new Scalar(0, 0, 255));
//
//		// Were both strips of tape detected?
//		if (contours.size() >= 2) {
//			Imgproc.drawContours(input, contours, 0, new Scalar(255, 0, 0));
//			Imgproc.drawContours(input, contours, 1, new Scalar(0, 255, 0));
//			// Calculate bounding rectangles of contours to compare x position
//			Rect oneRect = Imgproc.boundingRect(contours.get(0)), twoRect = Imgproc.boundingRect(contours.get(1));
//			double oneArea = oneRect.area(), twoArea = twoRect.area();
//
//			if(VisionPreferences.isDynamicTracking()){
//				if (oneArea<=twoArea){
//					trackingLeft = oneRect.x >= twoRect.x;
//					contours.remove(0);
//				} else {
//					trackingLeft = oneRect.x < twoRect.x;
//				}
//			} else {
//				if(trackingLeft == oneRect.x > twoRect.x){
//					contours.remove(0);
//				}
//			}
//		}
//
//		VisionPreferences.setTrackingLeft(trackingLeft);
//
//		// If no target found
//		if (contours.size() == 0){
//			return null;
//		}
//
//		// The first contour should be the optimal one
//		return contours.get(0);
//	}

	/**
	 * Determines the transformation of a camera relative to the vision target.
	 * The transformation is broken down into rotations and translations along the x, y, and z axes.
	 * Note that the rotation values are accurate while the translations are not - I'm still trying
	 * to figure out how to use the middle of the target as the reference point, instead of the top
	 * left corner.
	 *
	 * @param sourcePoints Corners of the tapes measured
	 * @param corners Corners of the tapes measured in the image
	 * @param input The image captured by the camera
	 * @return Three dimensional vector representing how close we are to target from the nexus
	 */
	public static Point3 getPosePnP(MatOfPoint3f sourcePoints, Point[] corners, Mat input) {

		final double depth = Constants.kPegLength, conv = 0.0393701 * 12 / 1.95;

		MatOfPoint2f dstPoints = new MatOfPoint2f();
		dstPoints.fromArray(corners);

		// In order to calculate the pose, we create a model of the vision targets using 3D coordinates
		MatOfDouble rvecs = new MatOfDouble(), tvecs = new MatOfDouble();
		Calib3d.solvePnP(
			sourcePoints,
			dstPoints,
			CameraInfo.IntrinsicMatrix(),
			CameraInfo.DistortionCoefficients(),
			rvecs,
			tvecs
		);
		MatOfPoint3f newPoints = new MatOfPoint3f(
			new Point3(0, 0, depth),
			new Point3(0, 0, 0    )
		);

		MatOfPoint2f result = new MatOfPoint2f();
		Calib3d.projectPoints(
			newPoints,
			rvecs,
			tvecs,
			CameraInfo.IntrinsicMatrix(),
			CameraInfo.DistortionCoefficients(),
			result
		);
		Point[] arr = result.toArray();

		// Estimates the position of the base and tip of the peg
		Imgproc.line(input, arr[0], arr[1], new Scalar(255, 255, 255), 5);

		for (Point p : arr) {
			Imgproc.circle(input, p, 7, new Scalar(0, 255, 0));
		}

		return new Point3(
			(tvecs.get(0, 0)[0]) * conv,
			(tvecs.get(1, 0)[0]) * conv,
			(tvecs.get(2, 0)[0]) * conv
		);
	}

	/**
	 * Identify four corners of the contour.
	 */
	public static Point[] getCorners(final MatOfPoint contour, final int shift) {
		Point[] arr = contour.toArray(), corners = new Point[4];
		Arrays.sort(arr, new Comparator<Point>() {
			public int compare(Point p1, Point p2) {
				return (int)((p1.x + p1.y) - (p2.x + p2.y));
			}
		});
		corners[0] = arr[0];
		corners[3] = arr[arr.length-1];
		Arrays.sort(arr, new Comparator<Point>() {
			public int compare(Point p1, Point p2) {
				return (int)((p1.x - p1.y) - (p2.x - p2.y));
			}
		});
		corners[2] = arr[0];
		corners[1] = arr[arr.length-1];
		for (int i = 0; i < 4; i++) corners[i].x -= shift;
		return corners;
	}

	public static String[] enumToString(Class<? extends Enum<?>> e){
		return Arrays.toString(e.getEnumConstants()).replaceAll("^.|.$", "").split(", ");
	}

	public static int getSetIndex(Set<? extends Object> set, Object value){
		if (set != null) {
			int idx = 0;
			for (Object o : set) {
				if (o.equals(value)) return idx;
				idx++;
			}
		}
		return -1;
	}

	/**
	 * Adds two arrays of the same type together
	 * @param a The first array
	 * @param b The second array
	 * @param <T> The type of both arrays
	 * @return The concatenated array
	 */
	public static <T> T[] concat(T[] a, T[] b) {
		@SuppressWarnings("unchecked")
		T[] result = (T[])Array.newInstance(a.getClass().getComponentType(), a.length + b.length);
		System.arraycopy(a, 0, result, 0, a.length);
		System.arraycopy(b, 0, result, a.length, b.length);
		return result;
	}
}
