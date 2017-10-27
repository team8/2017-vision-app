package com.frc8.team8vision.util;

public abstract class DataExistsCallback<T> {
	public boolean doesExist(T data){
		return data != null;
	}
}
