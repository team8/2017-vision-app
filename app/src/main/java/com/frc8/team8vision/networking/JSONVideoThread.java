package com.frc8.team8vision.networking;

import android.app.Activity;
import android.util.Base64;
import android.util.Log;

import com.frc8.team8vision.util.Constants;
import com.frc8.team8vision.vision.VisionInfoData;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Writes video data (current frame) to json file.
 *
 * @author Quintin Dwight
 */
public class JSONVideoThread extends AbstractJSONWriter {

    public static JSONVideoThread s_instance;

    /**
     * Creates a instance of this
     * Cannot be called outside as a Singleton
     */
    private JSONVideoThread() {
        super("JSONVideoThread", "frame.json");
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

        super.start(activity, Constants.kDataUpdateRateMS);
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

    @Override
    protected JSONObject getJSON() {

        final byte[] frame = VisionInfoData.getFrameAsByteArray();

        JSONObject json = new JSONObject();

        try {

            json.put("frame", Base64.encodeToString(frame, 0));

        } catch (JSONException e) {

            e.printStackTrace();
        }

        return json;
    }
}
