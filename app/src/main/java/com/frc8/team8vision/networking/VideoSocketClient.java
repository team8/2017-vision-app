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
 * Separate Thread to send image data to roboRIO
 * Strategies:
 *  - Write mat data to JSON, then read JSON
 *
 * <h1><b>Fields</b></h1>
 * 	<ul>
 * 		<li>Instance and State variables:
 * 			<ul>
 * 				<li>{@link VideoSocketClient#s_instance} (Singleton): Private static instance of this class</li>
 * 			    <li>{@link VideoSocketClient#m_activity} (private): Activity hosting the thread</li>
 * 			</ul>
 * 		</li>
 * 		<li>Utility variables:
 * 			<ul>
 * 				<li>{@link AbstractVisionThread#m_secondsAlive}: Private count of seconds the program has run for</li>
 * 			</ul>
 * 		</li>
 * 	</ul>
 *
 * <h1><b>Accessors and Mutators</b></h1>
 * 	<ul>
 * 		<li>{@link VideoSocketClient#getInstance()}</li>
 * 	</ul>
 *
 * <h1><b>External Access Functions</b>
 * 	<br><BLOCKQUOTE>For using as a wrapper for RIOdroid</BLOCKQUOTE></h1>
 * 	<ul>
 * 		<li>{@link VideoSocketClient#start(Activity)}</li>
 * 		<li>{@link AbstractVisionThread#pause()}</li>
 * 		<li>{@link AbstractVisionThread#resume()}</li>
 * 	</ul>
 *
 * 	<h1><b>Internal Functions</b>
 * 	<br><BLOCKQUOTE>Paired with external access functions. These compute the actual function for the external access</BLOCKQUOTE></h1>
 * 	<ul>
 * 		<li>{@link VideoSocketClient#writeFrameToSocket()}</li>
 * 	</ul>
 *
 * Created by Alvin on 2/16/2017.
 * @author Alvin
 */

public class VideoSocketClient extends AbstractVisionClient {

	// Instance and state variables
	private static VideoSocketClient s_instance;
	private Activity m_activity;

	// Utility variables
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
	public void start(Activity activity){

		m_activity = activity;

		super.start(Constants.kVisionUpdateRateMS, Constants.kRIOHostName, Constants.kVisionPortNumber, false);
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

