package com.frc8.team8vision.vision;

import com.frc8.team8vision.util.DataExistsCallback;

import org.opencv.core.Mat;

/**
 * Created by Alvin on 9/10/2017.
 */

public class VisionInfoData {

	private static VisionData<Double> x_dist = new VisionDataSynchronized<>("x_dist", Double.NaN, null,
			new DataExistsCallback<Double>() {
				@Override
				public boolean doesExist(Double data) {
					return !(data == null || data.isNaN() || data.isInfinite());
				}
			});
	private static VisionData<Double> z_dist = new VisionDataSynchronized<>("z_dist", Double.NaN, null,
			new DataExistsCallback<Double>() {
				@Override
				public boolean doesExist(Double data) {
					return !(data == null || data.isNaN() || data.isInfinite());
				}
			});
	private static VisionData<Mat> imageMat = new VisionDataSynchronized<>("frame", null, null,
			new DataExistsCallback<Mat>() {
				@Override
				public boolean doesExist(Mat data) {
					return !(data == null || data.empty());
				}
			});

	public static void setXDist(VisionData<Double> x_value) {
		x_dist.set(x_value);
	}
	public static void setZDist(VisionData<Double> z_value) {
		z_dist.set(z_value);
	}
	public static void setFrame(Mat image) {
		if (image != null) {
			imageMat.setDefaultValue(image);
		}
		imageMat.set(image);
	}

	public static Double getXDist() {
		return x_dist.get();
	}
	public static Double getZDist() {
		return z_dist.get();
	}
	public static Mat getFrame() {
		return imageMat.get();
	}
}
