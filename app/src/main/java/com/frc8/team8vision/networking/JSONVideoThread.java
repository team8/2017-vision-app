package com.frc8.team8vision.networking;

import android.util.Base64;

import com.frc8.team8vision.vision.VisionInfoData;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Writes video data (current frame) to json file.
 *
 * @author Quintin Dwight
 */
public class JSONVideoThread extends AbstractJSONWriter {

    public JSONVideoThread() {

        super("JSONVideoThread", "frame.json");
    }

    @Override
    protected void init() {}

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
