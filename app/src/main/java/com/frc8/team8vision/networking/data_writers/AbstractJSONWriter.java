package com.frc8.team8vision.networking.data_writers;

import android.content.Context;
import android.util.Log;

import com.frc8.team8vision.util.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * Writes information to a json file on the nexus.
 * The class that implements this needs to override {@link #getJSON()} in order to specify which json data.
 *
 * @author Quintin Dwight
 */
public abstract class AbstractJSONWriter extends AbstractVisionThread {

    protected final String k_fileName;

    public AbstractJSONWriter(final String k_threadName, final String k_fileName) {
        super(k_threadName);
        this.k_fileName = k_fileName;
    }

    /**
     * Takes JSONObject and writes data to file.
     *
     * @param json JSONObject storing vision data.
     */
    protected void writeJSONToFile(JSONObject json) {
        try {
            OutputStreamWriter osw = new OutputStreamWriter(m_activity.openFileOutput("data.json", Context.MODE_PRIVATE));
            osw.write(json.toString());
            osw.flush();
            osw.close();
        } catch (IOException e) {
            Log.e(k_tag, "Could not write data in JSON form: " + e.toString());
        }
    }

    /**
     * Writes json with only a state
     *
     * @param state The state to write
     */
    protected void writeOnlyStateToJSONFile(String state) {
        Log.i(Constants.kTAG, "Writing default values for JSON with state " + state + "...");

        JSONObject json = new JSONObject();
        try {
            json.put("state", state);
            writeJSONToFile(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the data to write to the local file.
     *
     * @return JSON object representing data
     */
    protected abstract JSONObject getJSON();

    @Override
    protected void update() {
        switch (m_threadState) {
            case RUNNING: {
                writeJSONToFile(getJSON());
                break;
            }
        }
    }
}
