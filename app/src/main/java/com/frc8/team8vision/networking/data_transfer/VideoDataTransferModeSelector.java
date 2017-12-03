package com.frc8.team8vision.networking.data_transfer;

import android.app.Activity;

import com.frc8.team8vision.networking.data_writers.JSONVideoThread;
import com.frc8.team8vision.networking.data_writers.VideoSocketClient;
import com.frc8.team8vision.networking.data_writers.VisionDataSocketClient;
import com.frc8.team8vision.util.Constants;

import java.net.ConnectException;

/**
 * Created by Alvin on 12/2/2017.
 */

public class VideoDataTransferModeSelector extends DataTransferModeSelector {

	private String threadName, filename;
	private int port;

	public VideoDataTransferModeSelector(Activity activity, String threadName, String filename, int port, boolean isTesting){
		super(activity, isTesting);
		this.threadName = threadName;
		this.filename = filename;
		this.port = port;
	}

	@Override
	protected void initMap(Activity activity, boolean isTesting) {
		JSONVideoThread json = new JSONVideoThread(threadName+"_json", filename);
		json.start(activity, Constants.kVisionUpdateRateMS);
		json.pause();

		VideoSocketClient socket = new VideoSocketClient(threadName+"_socket");
		socket.start(activity, Constants.kVisionUpdateRateMS, Constants.kRIOHostName, Constants.kVideoPort, isTesting);
		socket.pause();

		transferers.put(DataTransferMode.CAT_JSON, json);
		transferers.put(DataTransferMode.SOCKET, socket);
	}
}
