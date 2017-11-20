package com.frc8.team8vision.util.Constants;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;

/**
 * Created by Alvin on 11/20/2017.
 */

public class KalmanGains {

	public static final Mat kMeasurementNoise = createNoise(new Size(3,3), 1);
	public static final Mat kProcessNoise = createNoise(new Size(3,3), 0.5f);

	private static Mat createNoise(Size size, float standardDeviation){
		Mat gaussianNoise = new Mat(size, CvType.CV_8UC3);
		Core.randn(gaussianNoise, 0, standardDeviation);
		return gaussianNoise;
	}

}
