package com.frc8.team8vision.networking.data_transfer;

import android.app.Activity;

import com.frc8.team8vision.networking.data_writers.AbstractVisionThread;
import com.frc8.team8vision.networking.data_writers.JSONVideoThread;
import com.frc8.team8vision.networking.data_writers.JSONVisionDataThread;
import com.frc8.team8vision.networking.data_writers.VideoSocketClient;
import com.frc8.team8vision.networking.data_writers.VisionDataSocketClient;
import com.frc8.team8vision.util.Constants;

import java.util.HashMap;

/**
 * Selector for data transfer modes of both vision data and video.
 *
 * @author Alvin On
 */
public abstract class DataTransferModeSelector {

    public enum DataTransferMode {
        CAT_JSON, SOCKET
    }

	protected HashMap<DataTransferMode, AbstractVisionThread> transferers = new HashMap<>();
	protected DataTransferMode currentMode = DataTransferMode.CAT_JSON;

	public DataTransferModeSelector(Activity activity, boolean isTesting) {
		initMap(activity, isTesting);
	}

	/**
	 * Initialize the hash map with the correct transfer classes
	 *
	 * @param activity The main activity
	 * @param isTesting Whether or not we are testing, used for starting threads
	 */
	protected abstract void initMap(Activity activity, boolean isTesting);

	public void setTransfererMode(DataTransferMode newMode) {
		transferers.get(currentMode).pause();
		transferers.get(newMode).resume();

		currentMode = newMode;
	}

	public AbstractVisionThread getTransferer() {
		return transferers.get(currentMode);
	}

	public void stopAll() {
		for (AbstractVisionThread thread : transferers.values()) {
			thread.stop();
		}
	}
}
