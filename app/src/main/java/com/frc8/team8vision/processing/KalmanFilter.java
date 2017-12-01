package com.frc8.team8vision.processing;

import android.util.Log;

import com.frc8.team8vision.android.CameraInfo;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point3;
import org.opencv.core.Size;

/**
 * Created by Alvin on 11/19/2017.
 */

public class KalmanFilter {

	// Dimension of one input of the state
	private final int INPUT_DIM = 3;

	/*
		X: state matrix
		Z: sensor update matrix
		P: covariance matrix
		K: kalman gain matrix
	 */
	private Mat X;
	private Mat Z;
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

	private boolean updated = false;

	public KalmanFilter(float[] predictionCovariance, Mat measurementNoise, Mat processNoise){
		H = Mat.eye(new Size(INPUT_DIM*2,INPUT_DIM*2), CvType.CV_32F);
		K = new Mat(new Size(INPUT_DIM*2,INPUT_DIM*2), CvType.CV_32F);
		F = getStateUpdateMat(CameraInfo.getCycleTime()/1000.f);

		P = initializeCovarianceMat(predictionCovariance);

		Q = processNoise;
		R = measurementNoise;
	}

	public void update(Point3 sensorUpdate){
		float cycleTime = CameraInfo.getCycleTime()/1000.0f;
		updateStateUpdatemat(F, cycleTime);

		if(!updated){
			Z = initializeVector((float)sensorUpdate.x, (float)sensorUpdate.y, (float)sensorUpdate.z, 0, 0, 0);
			X = Z.clone();
			updated = true;
		} else {
			updateVector(Z, sensorUpdate, cycleTime);
		}

		Mat Mtemp = new Mat(new Size(INPUT_DIM*2,INPUT_DIM*2), CvType.CV_32F);
		Mat Vtemp = new Mat(new Size(1,INPUT_DIM*2), CvType.CV_32F);

//		X = F*X
		matMult(F,X,X);

//		P = F*P*F' + Q
		matMult(F,P,Mtemp);
		matMult(Mtemp,F.t(),Mtemp);
		matAdd(Mtemp,Q,1,P);

		printMat("H: ",H);
		printMat("P: ",P);
//		K = P*H'(H*P*H' + R)^-1
		matMult(H,P,Mtemp);
		matMult(Mtemp,H.t(),Mtemp);
//		printMat("Temp: ",Mtemp);
		matAdd(Mtemp,R,1,K);
//		printMat("K: ",K);
		matMult(P,H.t(),Mtemp);
//		printMat("Temp: ",Mtemp);
		matMult(Mtemp,K.inv(),K);

		printMat("K: ",K);

//		X = X + K(Z-H*X)
		matMult(H,X,Vtemp);
		matAdd(Z,Vtemp,-1,Vtemp);
//		printVec("Temp: ",Vtemp);
		matMult(K,Vtemp,Vtemp);
		matAdd(X,Vtemp,1,X);
//		printVec("Temp: ",Vtemp);

//		P = P - K*H*P
		matMult(K,H,Mtemp);
		matMult(Mtemp,P,Mtemp);
		Core.subtract(P,Mtemp,P);
		matAdd(P,Mtemp,-1,P);

		printVec("Sensor: ",Z);
		printVec("State: ",X);

	}

	private Mat getStateUpdateMat(float cycleTime) {
		Mat tempF = Mat.eye(new Size(INPUT_DIM * 2, INPUT_DIM * 2), CvType.CV_32F);
		updateStateUpdatemat(tempF, cycleTime);
		return tempF;
	}

	private void updateStateUpdatemat(Mat updateMat, float cycleTime) {
		for(int i=0; i<INPUT_DIM; i++){
			updateMat.put(i,i+INPUT_DIM,cycleTime);
		}
	}

	private static Mat matMult(Mat input1, Mat input2, Mat result){
		Core.gemm(input1, input2, 1, Mat.zeros(result.size(), CvType.CV_32F), 0, result);
		return result;
	}

	private static Mat matAdd(Mat input1, Mat input2, double scale2, Mat result){
		Core.addWeighted(input1, 1, input2, scale2, 0, result);
		return result;
	}

	private void printVec(String prefix, Mat m){
		if(null == m || m.empty()){
			Log.d("______KALMAN_V________", prefix+"EMPTY MAT");
		} else {
			double px = m.get(0,0)[0];
			double py = m.get(1,0)[0];
			double pz = m.get(2,0)[0];
			double vx = m.get(3,0)[0];
			double vy = m.get(4,0)[0];
			double vz = m.get(5,0)[0];
			Log.d("______KALMAN_V________", String.valueOf(prefix+"<"+px+","+py+","+pz+","+vx+","+vy+","+vz+">"));
		}
	}

	private void printMat(String prefix, Mat m){
		if(null == m || m.empty()){
			Log.d("______KALMAN_M________", prefix+"EMPTY MAT");
		} else {
			Log.d("______KALMAN_M________", prefix+"\n"
					+ "<"+m.get(0,0)[0]+","+m.get(0,1)[0]+","+m.get(0,2)[0]+","+m.get(0,3)[0]+","+m.get(0,4)[0]+","+m.get(0,5)[0]+">"+"\n"
					+ "<"+m.get(1,0)[0]+","+m.get(1,1)[0]+","+m.get(1,2)[0]+","+m.get(1,3)[0]+","+m.get(1,4)[0]+","+m.get(1,5)[0]+">"+"\n"
					+ "<"+m.get(2,0)[0]+","+m.get(2,1)[0]+","+m.get(2,2)[0]+","+m.get(2,3)[0]+","+m.get(2,4)[0]+","+m.get(2,5)[0]+">"+"\n"
					+ "<"+m.get(3,0)[0]+","+m.get(3,1)[0]+","+m.get(3,2)[0]+","+m.get(3,3)[0]+","+m.get(3,4)[0]+","+m.get(3,5)[0]+">"+"\n"
					+ "<"+m.get(4,0)[0]+","+m.get(4,1)[0]+","+m.get(4,2)[0]+","+m.get(4,3)[0]+","+m.get(4,4)[0]+","+m.get(4,5)[0]+">"+"\n"
					+ "<"+m.get(5,0)[0]+","+m.get(5,1)[0]+","+m.get(5,2)[0]+","+m.get(5,3)[0]+","+m.get(5,4)[0]+","+m.get(5,5)[0]+">"
			);
		}
	}

	private Mat initializeVector(float px, float py, float pz, float vx, float vy, float vz){
		Mat temp = Mat.zeros(new Size(1,INPUT_DIM*2), CvType.CV_32F);
		temp.put(0,0, px);
		temp.put(1,0, py);
		temp.put(2,0, pz);
		temp.put(3,0, vx);
		temp.put(4,0, vy);
		temp.put(5,0, vz);
		return temp;
	}

	private Mat updateVector(Mat last_state, Point3 curr_pos, float cycleTime){
		last_state.put(INPUT_DIM, 0, (curr_pos.x - last_state.get(0,0)[0])/cycleTime);
		last_state.put(INPUT_DIM+1, 0, (curr_pos.y - last_state.get(1,0)[0])/cycleTime);
		last_state.put(INPUT_DIM+1, 0, (curr_pos.z - last_state.get(2,0)[0])/cycleTime);
		return last_state;
	}

	private Mat initializeCovarianceMat(float[] standardDevs){
		float x = standardDevs[0], y = standardDevs[1], z = standardDevs[2];
		Mat temp = Mat.zeros(new Size(INPUT_DIM*2,INPUT_DIM*2), CvType.CV_32F);
		temp.put(0,0,x*x);
		temp.put(1,1,y*y);
		temp.put(2,2,z*z);
		return temp;
	}

	public Point3 getState(){
		return new Point3(X.get(0,0)[0], X.get(1,0)[0], X.get(2,0)[0]);
	}

}
