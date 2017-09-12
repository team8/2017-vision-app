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

import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * Separate Thread to send image data to roboRIO
 * Strategies:
 *  - Write mat data to JSON, then read JSON
 *
 * <h1><b>Fields</b></h1>
 * 	<ul>
 * 		<li>Instance and State variables:
 * 			<ul>
 * 				<li>{@link JSONVisionDataThread#s_instance} (Singleton): Private static instance of this class</li>
 * 				<li>{@link JSONVisionDataThread#m_dataThreadState} (private): Current state of connection</li>
 * 			    <li>{@link JSONVisionDataThread#m_lastThreadState} (private): Last connection state</li>
 * 			    <li>{@link JSONVisionDataThread#m_writeState} (private): Current writing state</li>
 * 			    <li>{@link JSONVisionDataThread#m_activity} (private): Activity hosting the thread</li>
 * 				<li><b>See:</b>{@link DataThreadState}</li>
 * 			</ul>
 * 		</li>
 * 		<li>Utility variables:
 * 			<ul>
 * 				<li>{@link AbstractVisionThread#m_secondsAlive}: Private count of seconds the program has run for</li>
 * 				<li>{@link JSONVisionDataThread#m_writerInitialized}: Private boolean representing whether the writer for the current writing state has been initialized</li>
 * 				<li>{@link AbstractVisionThread#m_isRunning}: Private boolean representing whether the thread is running</li>
 * 			</ul>
 * 		</li>
 * 	</ul>
 *
 * <h1><b>Accessors and Mutators</b></h1>
 * 	<ul>
 * 		<li>{@link JSONVisionDataThread#getInstance()}</li>
 * 		<li>{@link JSONVisionDataThread#setState(DataThreadState)}</li>
 * 		<li>{@link JSONVisionDataThread#setWriteState(WriteState)}</li>
 * 	</ul>
 *
 * <h1><b>External Access Functions</b>
 * 	<br><BLOCKQUOTE>For using as a wrapper for RIOdroid</BLOCKQUOTE></h1>
 * 	<ul>
 * 		<li>{@link JSONVisionDataThread#start(Activity, WriteState)}</li>
 * 		<li>{@link JSONVisionDataThread#sendBroadcast()}</li>
 * 		<li>{@link JSONVisionDataThread#pause()}</li>
 * 		<li>{@link JSONVisionDataThread#resume()}</li>
 * 		<li>{@link JSONVisionDataThread#destroy()}</li>
 * 	</ul>
 *
 * 	<h1><b>Internal Functions</b>
 * 	 <br><BLOCKQUOTE>Paired with external access functions. These compute the actual function for the external access</BLOCKQUOTE></h1>
 * 	 <ul>
 * 	     <li>{@link JSONVisionDataThread#initializeWriter()}</li>
 * 	     <li>{@link JSONVisionDataThread#writeVisionData()}</li>
 * 	 </ul>
 *
 * @see DataThreadState
 * @see WriteState
 * 	 </ul>
 *
 * Created by Alvin on 2/16/2017.
 * @author Alvin
 */

public class JSONVisionDataThread extends AbstractVisionThread {

    private enum DataThreadState{
        PREINIT, IDLE, WRITING, INITIALIZE_WRITER, PAUSED
    }

    /**
     *  State of the writer
     *
     *  <ul>
     *      <li>{@link WriteState#IDLE}</li>
     *      <li>{@link WriteState#JSON}</li>
     *      <li>{@link WriteState#BROADCAST}</li>
     *      <li>{@link WriteState#BROADCAST_IDLE}</li>
     *  </ul>
     */
    public enum WriteState{
        IDLE, JSON, BROADCAST, BROADCAST_IDLE
    }

    // Instance and state variables
    public static JSONVisionDataThread s_instance;
    private DataThreadState m_dataThreadState = DataThreadState.PREINIT;
    private DataThreadState m_lastThreadState;
    private WriteState m_writeState = WriteState.IDLE;
    private Activity m_activity;

    // Utility variables
    private boolean m_writerInitialized = false;

    /**
     * Creates a JSONVisionDataThread instance
     * Cannot be called outside as a Singleton
     */
    private JSONVisionDataThread() {
        super("JSONVisionDataThread");
    }

    /**
     * @return The instance of the WTD
     */
    public static JSONVisionDataThread getInstance(){
        if(s_instance == null){
            s_instance = new JSONVisionDataThread();
        }
        return s_instance;
    }

    /**
     * Sets the state of the writer
     * @param state State to switch to
     */
    private void setState(DataThreadState state){
        if (m_dataThreadState != state) {
            try {
                Thread.sleep(Constants.kChangeStateWaitMS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            m_dataThreadState = state;
        }
    }

    /**
     * Sets the state of the writing
     * @param state State to switch to
     */
    private void setWriteState(WriteState state){
        if(m_writeState.equals(state)){
            Log.w(k_tag, "setWriteState Error:\n\tno change to write state");
        }else{
            m_writeState = state;
            m_writerInitialized = false;
            this.setState(DataThreadState.INITIALIZE_WRITER);
        }
    }

    /**
     * (Debug) Logs the Thread state
     */
    private void logThreadState(){
        Log.d(k_tag, "JSONVisionDataThread State: "+m_dataThreadState);
    }

    /**
     * (DEBUG) Logs the Write state
     */
    private void logWriteState(){
        Log.d(k_tag, "SocketConnection State: "+m_writeState);
    }

     /**
     * Starts the JSONVisionDataThread Thread
     * <br> start() will only set values, check for errors, and prepare for thread creation
     * <br> actual creation of thread completed in resume(), which is called during app startup
     * @param activity Parent activity of the Thread
     */
    public void start(Activity activity, WriteState writeState) {
        m_activity = activity;
        super.start(Constants.kDataUpdateRateMS);
        m_writeState = writeState;
    }

    @Override
    protected void init() {

        if(s_instance == null) { // This should never happen
            Log.e(k_tag, "start Error:\n\tNo initialized instance, this should never happen");
            return;
        }

        if(m_dataThreadState != DataThreadState.PREINIT) {
            Log.e(k_tag, "start Error:\n\tThread has already been initialized");
            logThreadState();
        }

        if(m_writeState != WriteState.IDLE) {
            Log.e(k_tag, "start SocketConnection Error:\n\tAlready in a writing state");
            logWriteState();
        }
    }

    /**
     * On receiving broadcast intent, send data
     */
    public void sendBroadcast(){
        if(!m_writeState.equals(WriteState.BROADCAST_IDLE)){
            Log.e(k_tag, "sendBroadcast Error:\n\tTrying to send broadcast when thread is not waiting to send");
        }else{
            m_writeState = WriteState.BROADCAST;
        }
    }

    /**
     * Pauses the Thread
     * <br>Called when the program pauses/releases the Camera
     */
    public void pause(){
        if (!m_isRunning)
            Log.e(k_tag, "pause Error:\n\tThread is not running");

        m_lastThreadState = m_dataThreadState;
        this.setState(DataThreadState.PAUSED);
        this.writeState("PAUSED");
    }

    /**
     * Either resumes or initializes the Thread<br>
     * <br>Two Possibilities
     * <ul>
     *     <li>
     *         Thread state is paused
     *         <br>Normally resume the thread from paused state
     *         <br>({@link DataThreadState#PAUSED})
     *     </li>
     *
     *     <li>
     *         Thread state is pre-initialization
     *         <br>Complete second part of thread initialization:
     *         <br><BREAKQUOTE>Set the thread state and start running the actual thread</BREAKQUOTE>
     *         <br>({@link DataThreadState#PAUSED})
     *     </li>
     * </ul>
     */
    public void resume(){
        if (m_dataThreadState.equals(DataThreadState.PAUSED)) {   // If paused, continue from where we left off
            this.setState(m_lastThreadState);
        } else if (m_dataThreadState.equals(DataThreadState.PREINIT)) {  // If thread is initializing, finish initialization
            WriteState newState = m_writeState;
            m_writeState = WriteState.IDLE;
            this.setWriteState(newState);

            Log.i(k_tag, "resume Info:\n\tStarting Thread: JSONVisionDataThread");
            (new Thread(this, "JSONVisionDataThread")).start();
        } else {  // This should never happen
            Log.e(k_tag, "resume Error:\n\tTrying to resume a non-paused Thread");
            this.logThreadState();
        }
    }

    /**
     * Called when the thread is destroyed
     * <br><br>
     * <b>Note:</b>
     * if the app is not formally closed,
     * the WDT object will not be destroyed,
     * but thread will still stop and need
     * to be started again for use upon
     * re-opening the app
     */
    @Override
    protected void tearDown() {

        setWriteState(WriteState.IDLE);
        setState(DataThreadState.PREINIT);
    }

    /**
     * Initializes the selected method of writing data
     * @return The state after execution
     */
    private DataThreadState initializeWriter() {
        // FOR METHODS OTHER THAN JSON WRITING, EDIT
        switch (m_writeState){
            case IDLE:
                Log.e(k_tag, "initializeWriter Error:\n\tTrying to initialize an idle writer...");
                return DataThreadState.INITIALIZE_WRITER;
            case JSON:
                Log.i(k_tag, "initializeWriter Info:\n\tInitalizing JSON writer");
                break;  // Nothing to do for JSON mode
            case BROADCAST:
                Log.i(k_tag, "initializeWriter Info:\n\tInitializing Broadcast writer");
                break;
        }
        return DataThreadState.WRITING;
    }

    /**
     * Writes matrix data to JSON file
     * @return The state after execution
     */
    private DataThreadState writeVisionData() {
        HashMap<String, Object> data = new HashMap<>();
        data.put("state", "STREAMING");
        data.put("x_displacement", VisionInfoData.getXDist());
        data.put("z_displacement", VisionInfoData.getZDist());
        writeData(data);

        return m_dataThreadState;
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
     * Writes to JSON based on given keys and values
     * @param data Set of keys and values to populate the JSON file
     */
    private void writeData(HashMap<String,Object> data) {

        // Create JSONObject from given map data
        JSONObject json = new JSONObject();
        try {
            for (String key : data.keySet()) {
                json.put(key, data.get(key));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Choose writing method based on write state
        switch (m_writeState) {
            case IDLE:
                break;
            case JSON:
                writeJSON(json);
                break;
            case BROADCAST:
                writeSocket(json);
                m_writeState = WriteState.BROADCAST_IDLE;
                break;
            case BROADCAST_IDLE:
                // Do nothing, wait until broadcast is sent
                break;
        }
    }

    /**
     * Takes JSONObject and writes data to file
     *
     * @param json JSONObject storing vision data
     */
    private void writeJSON(JSONObject json){
        try {
            OutputStreamWriter osw = new OutputStreamWriter(m_activity.openFileOutput("data.json", Context.MODE_PRIVATE));
            osw.write(json.toString());
            osw.close();
        } catch (IOException e) {
            Log.e(k_tag, "writeJSON Error:\n\tCould not write data" + e.toString());
        }
    }

    /**
     * Takes JSONObject and writes data to socket
     * @param json JSONObject storing vision data
     */
    private void writeSocket(JSONObject json){
        try {
//            Log.d(TAG, "writeSocket Debug:\n\tOpening socket on "+ m_hostName +":"+ m_RIOPort);
            Socket socket = new Socket(Constants.kRIOHostName, Constants.kDataPortNumber);
//            Log.d(TAG, "writeSocket Debug:\n\tSocket Opened");
            OutputStreamWriter out = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
            out.write(json.toString());
            out.flush();
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes a state to the JSON object
     * @param state State to write
     */
    private void writeState(String state){
        HashMap<String, Object> state_info = new HashMap<>();
        state_info.put("state", state);
        writeData(state_info);
    }

    @Override
    protected void update() {

        switch (m_dataThreadState){

            case PREINIT:
                Log.e(k_tag, "Thread running, but has not been initialized");
                break;

            case INITIALIZE_WRITER:
                setState(initializeWriter());
                //writeState("STARTUP");
                break;

            case WRITING:
                setState(writeVisionData());
                break;

            case PAUSED:
                break;

        }
    }
}
