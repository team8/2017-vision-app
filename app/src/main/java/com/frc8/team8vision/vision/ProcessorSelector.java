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

	private HashMap<ProcessorType, VisionProcessorBase> processor_map = new HashMap<ProcessorType, VisionProcessorBase>();
	private ProcessorType processor = null;

	public VisionProcessorBase getProcessor(){
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
					processor_map.put(type, new CentroidProcessor());
					break;
				case SINGLE_TARGET:
					processor_map.put(type, new SingleTargetProcessor());
					break;
			}
		}
	}
}
