package com.frc8.team8vision.networking;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStreamWriter;

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
