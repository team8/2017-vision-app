package com.frc8.team8vision.networking;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.frc8.team8vision.util.Constants;
import com.frc8.team8vision.vision.VisionInfoData;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStreamWriter;

import java.util.HashMap;

/**
 * Writes image data to JSON to be read by the RoboRIO
 *
 * <h1><b>Fields</b></h1>
 * 	<ul>
 * 		<li>Instance and State variables:
 * 			<ul>
 * 				<li>{@link JSONVisionDataThread#s_instance} (Singleton): Private static instance of this class</li>
 * 			    <li>{@link JSONVisionDataThread#m_activity} (private): Activity hosting the thread</li>
 * 			</ul>
 * 		</li>
 * 		<li>Utility variables:
 * 			<ul>
 * 				<li>{@link AbstractVisionThread#m_secondsAlive}: Private count of seconds the program has run for</li>
 * 				<li>{@link AbstractVisionThread#m_isRunning}: Private boolean representing whether the thread is running</li>
 * 			</ul>
 * 		</li>
 * 	</ul>
 *
 * <h1><b>Accessors and Mutators</b></h1>
 * 	<ul>
 * 		<li>{@link JSONVisionDataThread#getInstance()}</li>
 * 	</ul>
 *
 * <h1><b>External Access Functions</b>
 * 	<br><BLOCKQUOTE>For using as a wrapper for RIOdroid</BLOCKQUOTE></h1>
 * 	<ul>
 * 		<li>{@link JSONVisionDataThread#start(Activity)}
 * 		<li>{@link AbstractVisionThread#pause()}</li>
 * 		<li>{@link AbstractVisionThread#resume()}</li>
 * 	</ul>
 *
 * 	<h1><b>Internal Functions</b>
 * 	 <br><BLOCKQUOTE>Paired with external access functions. These compute the actual function for the external access</BLOCKQUOTE></h1>
 * 	 <ul>
 * 	     <li>{@link JSONVisionDataThread#writeVisionData()}</li>
 * 	 </ul>
 **
 * Created by Alvin on 2/16/2017.
 * @author Alvin
 */

public class JSONVisionDataThread extends AbstractVisionThread {

    // Instance and state variables
    public static JSONVisionDataThread s_instance;
    private Activity m_activity;

    /**
     * Creates a instance of this
     * Cannot be called outside as a Singleton
     */
    private JSONVisionDataThread() {
        super("JSONVisionDataThread");
    }

    /**
     * @return The instance of the singleton
     */
    public static JSONVisionDataThread getInstance(){
        if(s_instance == null)
            s_instance = new JSONVisionDataThread();
        return s_instance;
    }

    /**
     * Start the thread.
     *
     * @param activity The current android activity.
     */
    public void start(Activity activity) {

        m_activity = activity;

        super.start(Constants.kDataUpdateRateMS);
    }

    @Override
    protected void init() {

        if (s_instance == null) {
            Log.e(k_tag, "No initialized instance in start, this should never happen!");
        }
    }

    @Override
    protected void onPause() {}

    @Override
    public void onResume() {}

    @Override
    public void onStop() {}

    /**
     * Writes matrix data to JSON file
     */
    private void writeVisionData() {

        final JSONObject json = VisionInfoData.getJsonRepresentation();
        writeJSON(json);
    }

//    /**
//     * Converts Matrix representing image data to byte array
//     * @param image Image data
//     * @return Computed byte array
//     */
//    private byte[] toByteArray(Mat image) {
//        byte[] retval = new byte[image.rows() * image.cols() * image.channels() * 8];
//        int index = 0;
//        for (int i = 0; i < image.rows(); i++) {
//            for (int j = 0; j < image.cols(); j++) {
//                double[] pixel = image.get(i, j);
//                for (int k = 0; k < image.channels(); k++) {
//                    byte[] bytes = new byte[8];
//                    ByteBuffer.wrap(bytes).putDouble(pixel[k]);
//                    for (byte b : bytes) retval[index++] = b;
//                }
//            }
//        }
//        return retval;
//    }

    /**
     * Takes JSONObject and writes data to file.
     *
     * @param json JSONObject storing vision data.
     */
    private void writeJSON(JSONObject json){
        try {
            OutputStreamWriter osw = new OutputStreamWriter(m_activity.openFileOutput("data.json", Context.MODE_PRIVATE));
            osw.write(json.toString());
            osw.close();
        } catch (IOException e) {
            Log.e(k_tag, "Could not write data in JSON form: " + e.toString());
        }
    }

//    /**
//     * Takes JSONObject and writes data to socket
//     * @param json JSONObject storing vision data
//     */
//    private void writeSocket(JSONObject json){
//        try {
////            Log.d(TAG, "writeSocket Debug:\n\tOpening socket on "+ m_hostName +":"+ m_RIOPort);
//            Socket socket = new Socket(Constants.kRIOHostName, Constants.kDataPortNumber);
////            Log.d(TAG, "writeSocket Debug:\n\tSocket Opened");
//            OutputStreamWriter out = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
//            out.write(json.toString());
//            out.flush();
//            out.close();
//            socket.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * Writes a state to the JSON object
//     * @param state State to write
//     */
//    private void writeState(String state) {
//
//        HashMap<String, Object> state_info = new HashMap<>();
//        state_info.put("state", state);
//        writeData(state_info);
//    }

    @Override
    protected void update() {

        switch (m_threadState) {

            case RUNNING: {
                writeVisionData();
                break;
            }
        }
    }
}
