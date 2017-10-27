package com.frc8.team8vision.networking;


import com.frc8.team8vision.vision.VisionInfoData;

import org.json.JSONObject;

/**
 * Writes image data to JSON to be read by the RoboRIO
 *
 * @author Quintin Dwight
 */
public class JSONVisionDataThread extends AbstractJSONWriter {

    public JSONVisionDataThread() {

        super("JSONVisionDataThread", "data.json");
    }

    @Override
    protected void init() {}

    @Override
    protected void onPause() {

        writeOnlyStateToJSONFile("PAUSED");
    }

    @Override
    public void onResume() {}

    @Override
    public void onStop() {

        writeOnlyStateToJSONFile("STOPPED");
    }

    @Override
    protected JSONObject getJSON() {

        return VisionInfoData.getJsonRepresentation();

    }
}
