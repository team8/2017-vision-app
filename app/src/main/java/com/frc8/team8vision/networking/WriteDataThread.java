package com.frc8.team8vision.networking;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.frc8.team8vision.Constants;
import com.frc8.team8vision.MainActivity;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.core.Mat;

import java.io.IOException;
import java.io.OutputStreamWriter;

import java.net.Socket;
import java.nio.ByteBuffer;
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
 * 				<li>{@link WriteDataThread#s_instance} (Singleton): Private static instance of this class</li>
 * 				<li>{@link WriteDataThread#m_dataThreadState} (private): Current state of connection</li>
 * 			    <li>{@link WriteDataThread#m_lastThreadState} (private): Last connection state</li>
 * 			    <li>{@link WriteDataThread#m_writeState} (private): Current writing state</li>
 * 			    <li>{@link WriteDataThread#m_activity} (private): Activity hosting the thread</li>
 * 				<li><b>See:</b>{@link DataThreadState}</li>
 * 			</ul>
 * 		</li>
 * 		<li>Utility variables:
 * 			<ul>
 * 				<li>{@link WriteDataThread#m_secondsAlive}: Private count of seconds the program has run for</li>
 * 				<li>{@link WriteDataThread#m_stateAliveTime}: Private count of seconds the state has run for</li>
 * 				<li>{@link WriteDataThread#m_writerInitialized}: Private boolean representing whether the writer for
 * 			                                                        the current writing state has been initialized	</li>
 * 				<li>{@link WriteDataThread#m_running}: Private boolean representing whether the thread is running</li>
 * 			</ul>
 * 		</li>
 * 	</ul>
 *
 * <h1><b>Accessors and Mutators</b></h1>
 * 	<ul>
 * 		<li>{@link WriteDataThread#getInstance()}</li>
 * 		<li>{@link WriteDataThread#SetState(DataThreadState)}</li>
 * 		<li>{@link WriteDataThread#SetWriteState(WriteState)}</li>
 * 	</ul>
 *
 * <h1><b>External Access Functions</b>
 * 	<br><BLOCKQUOTE>For using as a wrapper for RIOdroid</BLOCKQUOTE></h1>
 * 	<ul>
 * 		<li>{@link WriteDataThread#start(Activity, WriteState)}</li>
 * 		<li>{@link WriteDataThread#SendBroadcast()}</li>
 * 		<li>{@link WriteDataThread#pause()}</li>
 * 		<li>{@link WriteDataThread#resume()}</li>
 * 		<li>{@link WriteDataThread#destroy()}</li>
 * 	</ul>
 *
 * 	<h1><b>Internal Functions</b>
 * 	 <br><BLOCKQUOTE>Paired with external access functions. These compute the actual function for the external access</BLOCKQUOTE></h1>
 * 	 <ul>
 * 	     <li>{@link WriteDataThread#InitializeWriter()}</li>
 * 	     <li>{@link WriteDataThread#WriteVisionData()}</li>
 * 	 </ul>
 *
 * Created by Alvin on 2/16/2017.
 * @see DataThreadState
 * @see WriteState
 * 	 </ul>
 *
 * Created by Alvin on 2/16/2017.
 * @author Alvin
 */

public class WriteDataThread implements Runnable {

    /**
     *  State of the thread controlling the writer
     *
     *  <ul>
     *      <li>{@link DataThreadState#PREINIT}</li>
     *      <li>{@link DataThreadState#IDLE}</li>
     *      <li>{@link DataThreadState#WRITING}</li>
     *      <li>{@link DataThreadState#INITIALIZE_WRITER}</li>
     *      <li>{@link DataThreadState#PAUSED}</li>
     *  </ul>
     */
    public enum DataThreadState{
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

    // Tag
    private static final String TAG = Constants.kTAG+"WriteDataThread";

    // Instance and state variables
    public static WriteDataThread s_instance;
    private DataThreadState m_dataThreadState = DataThreadState.PREINIT;
    private DataThreadState m_lastThreadState;
    private WriteState m_writeState = WriteState.IDLE;
    private Activity m_activity;

    // Utility variables
    private double m_secondsAlive = 0;
    private double m_stateAliveTime = 0;
    private boolean m_writerInitialized = false;
    private boolean m_running = false;

    /**
     * Creates a WriteDataThread instance
     * Cannot be called outside as a Singleton
     */
    private WriteDataThread(){}

    /**
     * @return The instance of the WTD
     */
    public static WriteDataThread getInstance(){
        if(s_instance == null){
            s_instance = new WriteDataThread();
        }
        return s_instance;
    }

    /**
     * Sets the state of the writer
     * @param state State to switch to
     */
    private void SetState(DataThreadState state){
        if(!m_dataThreadState.equals(state)){
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
    private void SetWriteState(WriteState state){
        if(m_writeState.equals(state)){
            Log.w(TAG, "SetWriteState Error:\n\tno change to write state");
        }else{
            m_writeState = state;
            m_writerInitialized = false;
            this.SetState(DataThreadState.INITIALIZE_WRITER);
        }
    }

    /**
     * (Debug) Logs the Thread state
     */
    private void logThreadState(){
        Log.d(TAG, "WriteDataThread State: "+m_dataThreadState);
    }

    /**
     * (DEBUG) Logs the Write state
     */
    private void logWriteState(){
        Log.d(TAG, "SocketConnection State: "+m_writeState);
    }

    /**
     * Starts the WriteDataThread Thread
     * <br> start() will only set values, check for errors, and prepare for thread creation
     * <br> actual creation of thread completed in resume(), which is called during app startup
     * @param activity Parent activity of the Thread
     */
    public void start(Activity activity, WriteState writeState){
        if(s_instance == null) { // This should never happen
            Log.e(TAG, "start Error:\n\tNo initialized instance, this should never happen");
            return;
        }

        if(!m_dataThreadState.equals(DataThreadState.PREINIT)){
            Log.e(TAG, "start Error:\n\tThread has already been initialized");
            this.logThreadState();
        }

        if(!m_writeState.equals(WriteState.IDLE)){
            Log.e(TAG, "start SocketConnection Error:\n\tAlready in a writing state");
            this.logWriteState();
        }

        if(m_running){
            Log.e(TAG, "start Error:\n\tThread is already running");
            return;
        }
        m_activity = activity;
        m_running = true;

        // This line doesn't do anything, just prepares for changing the write state in resume()
        m_writeState = writeState;
    }

    /**
     * On receiving broadcast intent, send data
     */
    public void SendBroadcast(){
        if(!m_writeState.equals(WriteState.BROADCAST_IDLE)){
            Log.e(TAG, "SendBroadcast Error:\n\tTrying to send broadcast when thread is not waiting to send");
        }else{
            m_writeState = WriteState.BROADCAST;
        }
    }

    /**
     * Pauses the Thread
     * <br>Called when the program pauses/releases the Camera
     */
    public void pause(){
        if(!m_running){
            Log.e(TAG, "pause Error:\n\tThread is not running");
        }

        m_lastThreadState = m_dataThreadState;
        this.SetState(DataThreadState.PAUSED);
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
        if(m_dataThreadState.equals(DataThreadState.PAUSED)){   // If paused, continue from where we left off
            this.SetState(m_lastThreadState);
        }else if(m_dataThreadState.equals(DataThreadState.PREINIT)){  // If thread is initializing, finish initialization
            WriteState newState = m_writeState;
            m_writeState = WriteState.IDLE;
            this.SetWriteState(newState);

            Log.i(TAG, "resume Info:\n\tStarting Thread: WriteDataThread");
            (new Thread(this, "WriteDataThread")).start();
        }else{  // This should never happen
            Log.e(TAG, "resume Error:\n\tTrying to resume a non-paused Thread");
            this.logThreadState();
        }
    }

    /**
     * Destroys the Thread<br>
     * <br><b>Note:</b> if the app is not formally closed,
     *           the WDT object will not be destroyed,
     *           but thread will still stop and need
     *           to be started again for use upon
     *           re-opening the app
     */
    public void destroy(){
        m_running = false;
        this.SetWriteState(WriteState.IDLE);
        this.SetState(DataThreadState.PREINIT);
        this.writeState("TERMINATED");
    }

    /**
     * Initializes the selected method of writing data
     * @return The state after execution
     */
    private DataThreadState InitializeWriter(){
        // FOR METHODS OTHER THAN JSON WRITING, EDIT
        switch (m_writeState){
            case IDLE:
                Log.e(TAG, "InitializeWriter Error:\n\tTrying to initialize an idle writer...");
                return DataThreadState.INITIALIZE_WRITER;
            case JSON:
                Log.i(TAG, "InitializeWriter Info:\n\tInitalizing JSON writer");
                break;  // Nothing to do for JSON mode
            case BROADCAST:
                Log.i(TAG, "InitializeWriter Info:\n\tInitializing Broadcast writer");
                break;
        }
        return DataThreadState.WRITING;
    }

    /**
     * Writes matrix data to JSON file
     * @return The state after execution
     */
    private DataThreadState WriteVisionData(){
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("state", "STREAMING");
        data.put("x_displacement", MainActivity.getXDisplacement());
        data.put("z_displacement", MainActivity.getZDisplacement());
        writeData(data);

        return m_dataThreadState;
    }

    /**
     * Converts Matrix representing image data to byte array
     * @param image Image data
     * @return Computed byte array
     */
    private byte[] toByteArray(Mat image) {
        byte[] retval = new byte[image.rows() * image.cols() * image.channels() * 8];
        int index = 0;
        for (int i = 0; i < image.rows(); i++) {
            for (int j = 0; j < image.cols(); j++) {
                double[] pixel = image.get(i, j);
                for (int k = 0; k < image.channels(); k++) {
                    byte[] bytes = new byte[8];
                    ByteBuffer.wrap(bytes).putDouble(pixel[k]);
                    for (byte b : bytes) retval[index++] = b;
                }
            }
        }
        return retval;
    }

    /**
     * Writes to JSON based on given keys and values
     * @param data Set of keys and values to populate the JSON file
     */
    private void writeData(HashMap<String,Object> data){

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
<<<<<<< HEAD
     * @param json JSONObject storing vision data
=======
     * @param json
>>>>>>> Alvin On 2-16-17: Added a thread to write data (currently only via JSON
     */
    private void writeJSON(JSONObject json){
        try{
            OutputStreamWriter osw = new OutputStreamWriter(m_activity.openFileOutput("data.json", Context.MODE_PRIVATE));
            osw.write(json.toString());
            osw.close();
        } catch (IOException e){
            Log.e(TAG, "writeJSON Error:\n\tCould not write data" + e.toString());
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
        HashMap<String, Object> state_info = new HashMap<String, Object>();
        state_info.put("state", state);
        writeData(state_info);
    }

    /**
     * Updates the thread at {@link Constants#kVisionUpdateRateMS} ms
     */
    @Override
    public void run() {
        while(m_running){
            DataThreadState initState = m_dataThreadState;
            switch (m_dataThreadState){

                case PREINIT:   // This should never happen
                    Log.e(TAG, "WriteDataThread Error:\n\tThread running, but has not been initialized");
                    break;

                case INITIALIZE_WRITER:
                    this.SetState(this.InitializeWriter());
                    this.writeState("STARTUP");
                    break;

                case WRITING:
                    this.SetState(this.WriteVisionData());
                    break;

                case PAUSED:
                    break;

            }

            // Reset state start time if state changed
            if(!initState.equals(m_dataThreadState)){
                m_stateAliveTime = m_secondsAlive;
            }

            // Handle thread sleeping, sleep for set time delay
            try{
                Thread.sleep(Constants.kDataUpdateRateMS);
                m_secondsAlive += Constants.kDataUpdateRateMS /1000.0;
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    }
}
