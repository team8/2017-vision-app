package com.frc8.team8vision.networking;

import android.app.Activity;
import android.util.Log;

import com.frc8.team8vision.util.Constants;
import com.frc8.team8vision.vision.VisionInfoData;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Writes the current video frame to a socket to the RoboRIO
 *
 * @author Quintin Dwight
 */
public class VideoSocketClient extends AbstractVisionClient {

	private static VideoSocketClient s_instance;

	private double m_lastFrameTime = 0;

	/**
	 * Creates an instance of this
	 * Cannot be called outside as a Singleton
	 */
	private VideoSocketClient() {
		super("VideoSocketClient");
	}

	/**
	 * @return The instance of the singleton
	 */
	public static VideoSocketClient getInstance() {

		if(s_instance == null)
			s_instance = new VideoSocketClient();

		return s_instance;
	}

	/**
	 * Starts the VideoSocketClient Thread
	 *
	 * @param activity Parent activity of the Thread
	 */
	public void start(Activity activity) {

		super.start(activity, Constants.kVisionUpdateRateMS, Constants.kRIOHostName, Constants.kVisionPortNumber, false);
	}

	@Override protected void afterInit() {}

	/**
	 * Writes image matrix data to the socket.
	 */
	private void writeFrameToSocket() {

		// Initialize data
		byte[] imageData = VisionInfoData.getFrameAsByteArray();

		// Check if data is empty
		if (imageData != null && imageData.length != 0) {

			try {
				// Initialize data streams
				OutputStream out = m_client.getOutputStream();
				DataOutputStream dos = new DataOutputStream(out);

				dos.writeInt(imageData.length);
				dos.write(imageData, 0, imageData.length);

			} catch (IOException e) {

				Log.e(k_tag, "Cannot get output stream of socket!");
				closeSocket();
			}
		}
	}

	@Override
	public void afterUpdate() {

		switch (m_threadState) {

			case RUNNING: {

				switch (m_socketState) {

					case OPEN: {
						writeFrameToSocket();
						break;
					}
				}
			}
		}
	}
}

