package com.frc8.team8vision;

import android.app.Activity;
import android.content.Context;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
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
 * 				<li>{@link WriteDataThread#s_dataThreadState} (private): Current state of connection</li>
 * 			    <li>{@link WriteDataThread#s_lastThreadState} (private): Last connection state</li>
 * 			    <li>{@link WriteDataThread#s_activity} (private): Activity hosting the thread</li>
 * 				<li><b>See:</b>{@link DataThreadState}</li>
 * 			</ul>
 * 		</li>
 * 		<li>Utility variables:
 * 			<ul>
 * 				<li>{@link WriteDataThread#s_secondsAlive}: Private count of seconds the program has run for</li>
 * 				<li>{@link WriteDataThread#s_stateAliveTime}: Private count of seconds the state has run for</li>
 * 			    <li>{@link WriteDataThread#s_frameUpdateRateMS}: Private count of ms between frame updates</li>
 * 			    <li>{@link WriteDataThread#s_changeStateWaitMS}: Private count of ms to wait when switching states</li>
 * 				<li>{@link WriteDataThread#s_running}: Private boolean representing whether the thread is running</li>
 * 			</ul>
 * 		</li>
 * 	</ul>
 *
 * <h1><b>Accessors and Mutators</b></h1>
 * 	<ul>
 * 		<li>{@link WriteDataThread#getInstance()}</li>
 * 		<li>{@link WriteDataThread#SetState(DataThreadState)}</li>
 * 	</ul>
 *
 * <h1><b>External Access Functions</b>
 * 	<br><BLOCKQUOTE>For using as a wrapper for RIOdroid</BLOCKQUOTE></h1>
 * 	<ul>
 * 		<li>{@link WriteDataThread#start(Activity, WriteState)}</li>
 * 	</ul>
 *
 * 	<h1><b>Internal Functions</b>
 * 	 <br><BLOCKQUOTE>Paired with external access functions. These compute the actual function for the external access</BLOCKQUOTE></h1>
 * 	 <ul>
 * 	     <li>{@link WriteDataThread#InitializeWriter()}</li>
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
    public enum WriteState{
        IDLE, JSON, BROADCAST, BROADCAST_IDLE
    }

    // Instance and state variables
    public static WriteDataThread s_instance;
    private static DataThreadState s_dataThreadState = DataThreadState.PREINIT;
    private static DataThreadState s_lastThreadState;
    private static WriteState s_writeState = WriteState.IDLE;
    private static Activity s_activity;

    // Utility variables
    private static double s_secondsAlive = 0;
    private static double s_stateAliveTime = 0;
    private static long s_frameUpdateRateMS = 100;
    private static long s_changeStateWaitMS = 250;
    private static boolean s_writerInitialized = false;
    private static boolean s_running = false;
    private String hostName = "127.0.0.1";
    private static final int RIOPort = 8008;

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
        if(!s_dataThreadState.equals(state)){
            try {
                Thread.sleep(s_changeStateWaitMS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            s_dataThreadState = state;
        }
    }

    private void SetWriteState(WriteState state){
        if(s_writeState.equals(state)){
            Log.w("WriteState Error:", "no change to write state");
        }else{
            s_writeState = state;
            s_writerInitialized = false;
            this.SetState(DataThreadState.INITIALIZE_WRITER);
        }
    }

    public void SendBroadcast(){
        if(!s_writeState.equals(WriteState.BROADCAST_IDLE)){
            Log.e("WriteState Error", "Trying to send braodcast when thread is not waiting to send");
        }else{
            this.logThreadState();
            this.logWriteState();
            s_writeState = WriteState.BROADCAST;
        }
    }

    /**
     * (Debug) Logs the Thread state
     */
    private void logThreadState(){
        Log.d("WriteDataThread State", "ThreadState: "+s_dataThreadState);
    }

    private void logWriteState(){
        Log.d("WriteState state", "WriteState: "+s_writeState);
    }

    /**
     * Starts the WriteDataThread Thread
     * <br> start() will only set values, check for errors, and prepare for thread creation
     * <br> actual creation of thread completed in resume(), which is called during app startup
     * @param activity Parent activity of the Thread
     */
    public void start(Activity activity, WriteState writeState){
        if(s_instance == null) { // This should never happen
            Log.e("WriteDataThread Error", "No initialized instance, this should never happen");
            return;
        }

        if(!s_dataThreadState.equals(DataThreadState.PREINIT)){
            Log.e("WriteDataThread Error", "Thread has already been initialized");
            this.logThreadState();
        }

        if(!s_writeState.equals(WriteState.IDLE)){
            Log.e("WriteState Error", "Already in a writing state");
            this.logWriteState();
        }

        if(s_running){
            Log.e("WriteDataThread Error", "Thread is already running");
            return;
        }
        s_activity = activity;
        s_running = true;

        // This line doesn't do anything, just prepares for changing the write state in resume()
        s_writeState = writeState;
    }

    /**
     * Pauses the Thread
     * <br>Called when the program pauses/releases the Camera
     */
    public void pause(){
        if(!s_running){
            Log.e("WriteDataThread Error", "Thread is not running");
        }

        s_lastThreadState = s_dataThreadState;
        this.SetState(DataThreadState.PAUSED);
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
        if(s_dataThreadState.equals(DataThreadState.PAUSED)){   // If paused, continue from where we left off
            this.SetState(s_lastThreadState);
        }else if(s_dataThreadState.equals(DataThreadState.PREINIT)){  // If thread is initializing, finish initialization
            WriteState newState = s_writeState;
            s_writeState = WriteState.IDLE;
            this.SetWriteState(newState);

            Log.i("WriteDataThread Thread", "Starting Thread: WriteDataThread");
            (new Thread(this, "WriteDataThread")).start();
        }else{  // This should never happen
            Log.e("WriteDataThread Error", "Trying to resume a non-paused Thread");
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
        s_running = false;
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
        switch (s_writeState){
            case IDLE:
                Log.e("WriteState Error", "Trying to initialize an idle writer...");
                return DataThreadState.INITIALIZE_WRITER;
            case JSON:
                Log.i("WriteState Info", "Initalizing JSON writer");
                break;  // Nothing to do for JSON mode
            case BROADCAST:
                Log.i("WriteState Info", "Initializing Broadcast writer");
                break;
        }
        return DataThreadState.WRITING;
    }

    /**
     * Writes matrix data to JSON file
     * @return The state after execution
     */
    private DataThreadState WriteMatImage(){
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("state", "STREAMING");
//        String image_data = Base64.encodeToString(toByteArray(MainActivity.getImage()), Base64.DEFAULT);
        String image_data = "wow-data!";
        data.put("image_rgb", image_data);
        writeData(data);

        return DataThreadState.WRITING;
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

        JSONObject json = new JSONObject();
        try {
            for (String key : data.keySet()) {
                json.put(key, data.get(key));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        switch (s_writeState) {
            case IDLE:
                Log.e("WriteState Error", "Trying to write in idle write state");
                break;
            case JSON:
                writeJSON(json);
                break;
            case BROADCAST:
                writeSocket(json);
                s_writeState = WriteState.BROADCAST_IDLE;
                break;
            case BROADCAST_IDLE:
                // Do nothing, wait until broadcast is sent
                break;
        }
    }

    /**
     * Takes JSONObject and writes data to file
     * @param json
     */
    private void writeJSON(JSONObject json){
        try{
            OutputStreamWriter osw = new OutputStreamWriter(s_activity.openFileOutput("data.json", Context.MODE_PRIVATE));
            osw.write(json.toString());
            osw.close();
        } catch (IOException e){
            Log.e("Exception", "Could not write data" + e.toString());
        }
    }

    private void writeSocket(JSONObject json){
        try {
            Log.i("-----WriteSocket-----", "Opening socket on "+hostName+":"+RIOPort);
            Socket socket = new Socket(hostName, RIOPort);
            Log.i("-----WriteSocket-----", "Socket Opened");
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
     * Updates the thread at {@link WriteDataThread#s_frameUpdateRateMS} ms
     */
    @Override
    public void run() {
        while(s_running){
            DataThreadState initState = s_dataThreadState;
            switch (s_dataThreadState){

                case PREINIT:   // This should never happen
                    Log.e("WriteDataThread Error", "Thread running, but has not been initialized");
                    break;

                case INITIALIZE_WRITER:
                    this.SetState(this.InitializeWriter());
                    this.writeState("STARTUP");
                    break;

                case WRITING:
                    this.SetState(this.WriteMatImage());
                    break;

                case PAUSED:
                    this.writeState("PAUSED");
                    break;

            }

            // Reset state start time if state changed
            if(!initState.equals(s_dataThreadState)){
                s_stateAliveTime = s_secondsAlive;
            }

            // Handle thread sleeping, sleep for set time delay
            try{
                Thread.sleep(s_frameUpdateRateMS);
                s_secondsAlive += s_frameUpdateRateMS/1000.0;
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    }
}
