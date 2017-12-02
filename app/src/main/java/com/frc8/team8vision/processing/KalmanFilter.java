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
	private final int NUM_INPUTS = 1;
	private final int VEC_LEN = INPUT_DIM * NUM_INPUTS;

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
		H = Mat.eye(new Size(VEC_LEN,VEC_LEN), CvType.CV_32F);
		K = new Mat(new Size(VEC_LEN,VEC_LEN), CvType.CV_32F);
		F = getStateUpdateMat(CameraInfo.getCycleTime()/1000.f);

		P = initializeCovarianceMat(predictionCovariance);

		Q = processNoise;
		R = measurementNoise;
	}

	public void update(Point3 sensorUpdate){
		float cycleTime = CameraInfo.getCycleTime()/1000.0f;
		updateStateUpdateMat(F, cycleTime);

		if(!updated){
			Z = initializeVector(new float[]{(float)sensorUpdate.x, (float)sensorUpdate.y, (float)sensorUpdate.z, 0, 0, 0});
			X = Z.clone();
			updated = true;
		} else {
			updateFromPoint(Z, sensorUpdate);
		}

		Mat Mtemp = new Mat(new Size(VEC_LEN,VEC_LEN), CvType.CV_32F);
		Mat Vtemp = new Mat(new Size(1,VEC_LEN), CvType.CV_32F);

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

		printMat("Sensor: ",Z);
		printMat("State: ",X);

	}

	private Mat getStateUpdateMat(float cycleTime) {
		Mat tempF = Mat.eye(new Size(VEC_LEN, VEC_LEN), CvType.CV_32F);
		updateStateUpdateMat(tempF, cycleTime);
		return tempF;
	}

	private void updateStateUpdateMat(Mat updateMat, float cycleTime) {
		if(NUM_INPUTS<2) return;	// If not at least 2 inputs, skip
		float[] params = new float[NUM_INPUTS-1];
		for(int i=1; i>=NUM_INPUTS; i--){
			params[i-1] = (float)((1.0/i) * Math.pow(cycleTime, i));
		}
		for(int i=0; i<INPUT_DIM*(NUM_INPUTS-1); i++){
			for(int j=1; j<NUM_INPUTS; j++){
				if(j < NUM_INPUTS -(i%INPUT_DIM)){
					updateMat.put(i, i+INPUT_DIM*j, params[j-1]);
				}
			}
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

	private void printMat(String prefix, Mat m){
		if(null == m || m.empty()){
			Log.d("______KALMAN________", prefix+"EMPTY MAT");
		} else {
			StringBuilder out = new StringBuilder(prefix+"\n");
			for (int i=0; i<m.rows(); i++){
				out.append("<");
				for (int j=0; j<m.cols()-1; j++){
					out.append(m.get(i,j)[0] + ", ");
				}
				out.append(m.get(i, m.cols()-1)[0] + ">\n");
			}
			Log.d("______KALMAN_________", out.toString());
		}
	}

	private Mat initializeVector(float[] vec){
		if(vec.length != VEC_LEN){
			Log.e("KalmanFilter", "Mismatched vector lengths!");
			throw new IndexOutOfBoundsException("KalmanFilter: Mismatched vector lengths!");
		}
		Mat temp = Mat.zeros(new Size(1,VEC_LEN), CvType.CV_32F);
		for(int i=0; i<VEC_LEN; i++){
			temp.put(i,0,vec[i]);
		}
		return temp;
	}

	private void updateFromPoint(Mat last_state, Point3 input){
		last_state.put(0,0,input.x);
		last_state.put(1,0,input.x);
		last_state.put(2,0,input.x);
	}

	private Mat initializeCovarianceMat(float[] standardDevs){
		if(standardDevs.length != VEC_LEN){
			Log.e("KalmanFilter", "Mismatched vector lengths!");
			throw new IndexOutOfBoundsException("KalmanFilter: Mismatched vector lengths!");
		}
		Mat temp = Mat.zeros(new Size(VEC_LEN,VEC_LEN), CvType.CV_32F);
		for (int i=0; i<VEC_LEN; i++){
			temp.put(i,i,standardDevs[i]*standardDevs[i]);
		}
		return temp;
	}

	public Point3 getState(){
		return new Point3(X.get(0,0)[0], X.get(1,0)[0], X.get(2,0)[0]);
	}

}
