package com.frc8.team8vision.networking;

import android.app.Activity;
import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.frc8.team8vision.util.Constants;
import com.frc8.team8vision.vision.VisionInfoData;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * Writes video data (current frame) to json file.
 *
 * @author Quintin Dwight
 */
public class JSONVideoThread extends AbstractVisionThread {

    // Instance and state variables
    public static JSONVideoThread s_instance;
    private Activity m_activity;

    /**
     * Creates a instance of this
     * Cannot be called outside as a Singleton
     */
    private JSONVideoThread() {
        super("JSONVideoThread");
    }

    /**
     * @return The instance of the singleton
     */
    public static JSONVideoThread getInstance(){

        if(s_instance == null)
            s_instance = new JSONVideoThread();

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
     * Writes matrix data to JSON file.
     */
    private void writeVisionData() {

        final byte[] frame = VisionInfoData.getFrameAsByteArray();

        JSONObject json = new JSONObject();

        try {

            json.put("frame", Base64.encodeToString(frame, 0));

        } catch (JSONException e) {

            e.printStackTrace();
        }

        writeJSON(json);
    }

    /**
     * Takes JSONObject and writes data to file.
     *
     * @param json JSONObject that represents current frame
     */
    private void writeJSON(JSONObject json) {

        try {

            OutputStreamWriter osw = new OutputStreamWriter(m_activity.openFileOutput("video.json", Context.MODE_PRIVATE));
            osw.write(json.toString());
            osw.flush();
            osw.close();

        } catch (IOException e) {

            Log.e(k_tag, "Could not write data in JSON form: " + e.toString());
        }
    }

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
