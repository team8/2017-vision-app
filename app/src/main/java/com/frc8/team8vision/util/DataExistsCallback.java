package com.frc8.team8vision.util;

/**
 * Created by Alvin on 9/8/2017.
 */

public abstract class DataExistsCallback<T> {
	public boolean doesExist(T data){
		return data != null;
	}
}
