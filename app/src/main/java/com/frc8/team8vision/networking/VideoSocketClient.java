package com.frc8.team8vision.networking;

import android.util.Log;

import com.frc8.team8vision.vision.VisionInfoData;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Writes the current video frame to a socket to the RoboRIO
 *
 * @author Quintin Dwight
 */
public class VideoSocketClient extends AbstractVisionClient {

	private double m_lastFrameTime = 0;

	public VideoSocketClient() {

		super("VideoSocketClient");
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

				try {

					dos.writeInt(imageData.length);
					dos.write(imageData, 0, imageData.length);

				} catch (IOException e) {

					Log.e(k_tag, "Error writing to socket stream!");
					e.printStackTrace();
				}

			} catch (IOException e) {

				Log.e(k_tag, "Cannot get output stream of socket!");
				e.printStackTrace();

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

