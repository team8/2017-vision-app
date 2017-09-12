package com.frc8.team8vision.vision;

import com.frc8.team8vision.util.AutoCloseableLock;
import com.frc8.team8vision.util.DataExistsCallback;
import com.frc8.team8vision.util.ReadWriteLock;

/**
 * Created by Alvin on 9/10/2017.
 */

public class VisionDataSynchronized<T> extends VisionData<T>{

	private ReadWriteLock mLock;

	public VisionDataSynchronized(String name, T value, T default_value, DataExistsCallback<T> existsCallback) {
		super(value, default_value, existsCallback);
		mLock = new ReadWriteLock(name);
	}

	@Override
	public void set(T value){
		try (AutoCloseableLock lock = new AutoCloseableLock(mLock, ReadWriteLock.WRITING)){
			super.set(value);
		} catch (Exception e){
			// Handle these later lol
		}
	}
	@Override
	public void set(VisionData<T> v_data) {
		try (AutoCloseableLock lock = new AutoCloseableLock(mLock, ReadWriteLock.WRITING)){
			super.set(v_data);
		} catch (Exception e){
			// Handle these later lol
		}
	}
	@Override
	public void setDefaultValue(T value) {
		try (AutoCloseableLock lock = new AutoCloseableLock(mLock, ReadWriteLock.WRITING)){
			super.setDefaultValue(value);
		} catch (Exception e){
			// Handle these later lol
		}
	}
	@Override
	public void setToDefault() {
		try (AutoCloseableLock lock = new AutoCloseableLock(mLock, ReadWriteLock.WRITING)){
			super.setToDefault();
		} catch (Exception e){
			// Handle these later lol
		}
	}

	@Override
	public T get() {
		try (AutoCloseableLock lock = new AutoCloseableLock(mLock, ReadWriteLock.READING)){
			return super.get();
		} catch (Exception e){
			// Handle these later lol
			return super.getDefaultValue();
		}
	}
	@Override
	public T getRaw(){
		try (AutoCloseableLock lock = new AutoCloseableLock(mLock, ReadWriteLock.READING)){
			return super.getRaw();
		} catch (Exception e){
			// Handle these later lol
			return super.getDefaultValue();
		}
	}
	@Override
	public T getDefaultValue() {
		try (AutoCloseableLock lock = new AutoCloseableLock(mLock, ReadWriteLock.READING)){
			return super.getDefaultValue();
		} catch (Exception e){
			// Handle these later lol
			return super.getDefaultValue();
		}
	}

	@Override
	public boolean isNull(){
		try (AutoCloseableLock lock = new AutoCloseableLock(mLock, ReadWriteLock.READING)){
			return super.isNull();
		} catch (Exception e){
			// Handle these later lol
			return true;
		}
	}
	@Override
	public boolean exists() {
		try (AutoCloseableLock lock = new AutoCloseableLock(mLock, ReadWriteLock.READING)){
			return super.exists();
		} catch (Exception e){
			// Handle these later lol
			return false;
		}
	}
}
