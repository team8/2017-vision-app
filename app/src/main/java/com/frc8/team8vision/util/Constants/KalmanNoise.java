package com.frc8.team8vision.util.Constants;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;

import java.security.KeyManagementException;

/**
 * Created by Alvin on 11/20/2017.
 */

public class KalmanNoise {

//	public static final Mat kMeasurementNoise = createNoise(new Size(3,3), 1);
	public static final Mat kMeasurementNoise = Mat.zeros(new Size(3,3), CvType.CV_32F);
//	public static final Mat kProcessNoise = createNoise(new Size(3,3), 0.5f);
	public static final Mat kProcessNoise = Mat.zeros(new Size(3,3), CvType.CV_32F);

	static {
		kMeasurementNoise.put(0,0,0.1f);
		kMeasurementNoise.put(1,1,0.1f);
		kMeasurementNoise.put(2,2,0.1f);
	}

	private static Mat createNoise(Size size, float standardDeviation){
		Mat gaussianNoise = new Mat(size, CvType.CV_32F);
		Core.randn(gaussianNoise, 0, standardDeviation);
		return gaussianNoise;
	}

}
