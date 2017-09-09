package com.frc8.team8vision.vision;

/**
 * Created by Alvin on 9/8/2017.
 */

public abstract class DataExistsCallback<T> {
	public boolean doesExist(T data){
		return data == null;
	}
}
