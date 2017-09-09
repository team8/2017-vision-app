package com.frc8.team8vision.vision;

/**
 * Created by Alvin on 9/8/2017.
 */

public class VisionData<T> {
	private T data;
	private T default_value;
	private DataExistsCallback<T> callback;

	public VisionData(T value, T default_value, DataExistsCallback<T> existsCallback){
		this.data = value;
		this.default_value = default_value;
		this.callback = existsCallback;
	}

	public void set(T value){
		this.data = value;
	}
	public void setToDefault(){
		this.data = this.default_value;
	}
	public T get(){
		return this.data;
	}
	public T getDefault_value(){
		return this.default_value;
	}

	public boolean isNull() {
		return this.data == null;
	}
	public boolean exists(){
		return callback.doesExist(this.data);
	}
}
