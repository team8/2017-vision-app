package com.frc8.team8vision.android;

import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;

/**
 * Created by Alvin on 9/9/2017.
 */

public class CameraInfo {
	private static int mHeight, mWidth;

	private static Mat intrinsicMatrix;
	private static MatOfDouble distCoeffs;

	private static long cycleTime = 0;

	public static void setInfo(int height, int width, Mat intrinsics, MatOfDouble distortion){
		mHeight = height;
		mWidth = width;
		intrinsicMatrix = intrinsics;
		distCoeffs = distortion;
	}

	public static void setDims(int height, int width){
		mHeight = height;
		mWidth = width;
	}
	public static void setIntrinsics(Mat intrinsics){
		intrinsicMatrix = intrinsics;
	}
	public static void setDistortion(MatOfDouble distortion)
	{
		distCoeffs = distortion;
	}
	public static void updateCycleTime(long time) {
		cycleTime = time;
	}

	public static int Height(){
		return mHeight;
	}
	public static int Width(){
		return mWidth;
	}
	public static Mat IntrinsicMatrix(){
		return intrinsicMatrix;
	}
	public static MatOfDouble DistortionCoefficients(){
		return distCoeffs;
	}
	public static long getCycleTime() {
		return cycleTime;
	}

}
