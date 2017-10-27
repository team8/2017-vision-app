package com.frc8.team8vision.vision;

import com.frc8.team8vision.util.DataExistsCallback;

/**
 * Holds a unit of vision data
 *
 * @param <T>
 */
public class VisionDataUnit<T> {

	protected T data;
	protected T default_value;
	protected DataExistsCallback<T> callback;

	public VisionDataUnit(T value, T default_value, DataExistsCallback<T> existsCallback){
		this.data = value;
		this.default_value = default_value;
		this.callback = existsCallback;
	}

	public void set(T value){
		this.data = value;
	}
	public void set(VisionDataUnit<T> v_data){
		this.data = v_data.get();
	}
	public void setDefaultValue(T value){
		this.default_value = value;
	}
	public void setToDefault(){
		this.data = this.default_value;
	}

	public T get(){
		if(this.exists()) {
			return this.data;
		} else {
			return this.default_value;
		}
	}
	public T getRaw(){
		return this.data;
	}
	public T getDefaultValue(){
		return this.default_value;
	}

	public boolean isNull() {
		return this.data == null;
	}
	public boolean exists(){
		return callback.doesExist(this.data);
	}
}
