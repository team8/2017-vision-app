package com.frc8.team8vision.vision;

import com.frc8.team8vision.vision.processors.CentroidProcessor;
import com.frc8.team8vision.vision.processors.SingleTargetProcessor;

import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;

import java.util.HashMap;

/**
 * Created by Alvin on 9/8/2017.
 */

public class ProcessorSelector {
	public enum ProcessorType {
		CENTROID, SINGLE_TARGET
	};

	protected int mHeight, mWidth;
	protected Boolean trackingLeft;

	protected Mat intrinsicMatrix;
	protected MatOfDouble distCoeffs;

	private HashMap<ProcessorType, AbstractVisionProcessor> processor_map = new HashMap<ProcessorType, AbstractVisionProcessor>();
	private ProcessorType processor = null;

	public ProcessorSelector(int height, int width, Mat intrinsics, MatOfDouble distortion, boolean isTrackingLeft){
		mHeight = height;
		mWidth = width;
		intrinsicMatrix = intrinsics;
		distCoeffs = distortion;
		trackingLeft = isTrackingLeft;
	}

	public AbstractVisionProcessor getProcessor(){
		if(processor == null){
			this.setProcessor(ProcessorType.CENTROID);
		}
		return processor_map.get(this.processor);
	}

	public void setProcessor(ProcessorType type){
		this.processor = type;
		if(!processor_map.containsKey(type)){
			switch (type){
				case CENTROID:
					processor_map.put(type, new CentroidProcessor(mHeight, mWidth, intrinsicMatrix, distCoeffs, trackingLeft));
					break;
				case SINGLE_TARGET:
					processor_map.put(type, new SingleTargetProcessor(mHeight, mWidth, intrinsicMatrix, distCoeffs, trackingLeft));
					break;
			}
		}
	}
}
