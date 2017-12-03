package com.frc8.team8vision.networking.data_transfer;

import android.app.Activity;

import com.frc8.team8vision.networking.data_writers.JSONVideoThread;
import com.frc8.team8vision.networking.data_writers.JSONVisionDataThread;
import com.frc8.team8vision.networking.data_writers.VideoSocketClient;
import com.frc8.team8vision.networking.data_writers.VisionDataSocketClient;
import com.frc8.team8vision.util.Constants;

/**
 * Created by Alvin on 12/2/2017.
 */

public class VisionDataTransferModeSelector extends DataTransferModeSelector {

	private String threadName, filename;
	private int port;

	public VisionDataTransferModeSelector(Activity activity, String threadName, String filename, int port, boolean isTesting){
		super(activity, isTesting);
		this.threadName = threadName;
		this.filename = filename;
		this.port = port;
	}

	@Override
	protected void initMap(Activity activity, final boolean isTesting) {
		JSONVisionDataThread json = new JSONVisionDataThread(threadName+"_json", filename);
		json.start(activity, Constants.kDataUpdateRateMS);
		json.pause();

		VisionDataSocketClient socket = new VisionDataSocketClient(threadName+"_socket");
		socket.start(activity, Constants.kVisionUpdateRateMS, Constants.kRIOHostName, Constants.kVideoPort, isTesting);
		socket.pause();

		transferers.put(DataTransferMode.CAT_JSON, json);
		transferers.put(DataTransferMode.SOCKET, socket);
	}
}
