package com.frc8.team8vision.processing;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.Point3;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

/**
 * Created by Alvin on 11/19/2017.
 */

public class KalmanFilter {

	/*
		x: state matrix
		P: covariance matrix
		K: kalman gain matrix
	 */
	private Mat X;
	private Mat P;
	private Mat K;

	/*
		F: state update matrix
		Q: environment uncertainty matrix/process noise
		H: sensor model matrix
		R: sensor covariance/measurement noise
	 */
	private Mat F;
	private Mat Q;
	private Mat H;
	private Mat R;

	public KalmanFilter(float[] predictionCovariance, Mat measurementNoise, Mat processNoise){
		F = Mat.eye(new Size(3,3), F.type());
		H = Mat.eye(new Size(3,3), H.type());

		X = initializeVector(0, 0, 0);
		P = initializeCovarianceMat(predictionCovariance);

		Q = processNoise;
		R = measurementNoise;
	}

	public void update(Mat sensorUpdate){
		X =  F.mul(X);
		Core.add(F.mul(P).mul(F.t()), Q, P);

		Mat temp = new Mat();
		Core.add(H.mul(P).mul(H.t()), R, temp);
		K = P.mul(H.t().mul(temp.inv()));

		Core.subtract(sensorUpdate, H.mul(X), temp);
		Core.add(X, temp, X);
		Core.subtract(P, K.mul(H).mul(P), P);
	}

	private Mat initializeVector(float x, float y, float z){
		Mat temp = Mat.zeros(new Size(1, 3), X.type());
		temp.put(0,0, x);
		temp.put(0,1, y);
		temp.put(0,2, z);
		return temp;
	}

	private Mat initializeCovarianceMat(float[] standardDevs){
		float x = standardDevs[0], y = standardDevs[1], z = standardDevs[2];
		Mat temp = Mat.zeros(new Size(3,3), P.type());
		temp.put(0,0,x*x);
		temp.put(1,1,y*y);
		temp.put(2,2,z*z);
		return temp;
	}

	public Point3 getState(){
		return new Point3(X.get(0,0)[0], X.get(1,0)[0], X.get(2,0)[0]);
	}

}
