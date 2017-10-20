package com.frc8.team8vision.vision;

import com.frc8.team8vision.util.DataExistsCallback;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.core.Mat;

import java.util.HashMap;

/**
 * Contains various vision info data to be referenced in a static context
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

	public static JSONObject getJsonRepresentation() {

		JSONObject json = new JSONObject();
		try {
			json.put("x_displacement", VisionInfoData.getXDist());
			json.put("z_displacement", VisionInfoData.getZDist());
			return json;
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
	}
}
