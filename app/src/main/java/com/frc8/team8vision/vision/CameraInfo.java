package com.frc8.team8vision.vision;

import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;

/**
 * Created by Alvin on 9/9/2017.
 */

public class CameraInfo {
	private static int mHeight, mWidth;
	private static Boolean trackingLeft;

	private static Mat intrinsicMatrix;
	private static MatOfDouble distCoeffs;

	public static void setInfo(int height, int width, boolean isTrackingLeft, Mat intrinsics, MatOfDouble distortion){
		mHeight = height;
		mWidth = width;
		trackingLeft = isTrackingLeft;
		intrinsicMatrix = intrinsics;
		distCoeffs = distortion;
	}

	public static void setDims(int height, int width){
		mHeight = height;
		mWidth = width;
	}
	public static void setIsLeftTarget(boolean isTrackingLeft){
		trackingLeft = isTrackingLeft;
	}
	public static void setIntrinsics(Mat intrinsics){
		intrinsicMatrix = intrinsics;
	}
	public static void setDistortion(MatOfDouble distortion){
		distCoeffs = distortion;
	}

	public static int Height(){
		return mHeight;
	}
	public static int Width(){
		return mWidth;
	}
	public static boolean isTrackingLeft(){
		return trackingLeft;
	}
	public static Mat IntrinsicMatrix(){
		return intrinsicMatrix;
	}
	public static MatOfDouble DistortionCoefficients(){
		return distCoeffs;
	}
}
