package com.frc8.team8vision.networking;

import com.frc8.team8vision.vision.VisionInfoData;

import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * Sends vision information (x and z distance) through a socket to the RoboRIO.
 */
public class VisionDataSocketClient extends AbstractVisionClient {

    private static VisionDataSocketClient s_instance;

    /**
     * @return The instance of the singleton
     */
    public static VisionDataSocketClient getInstance() {

        if (s_instance == null)
            s_instance = new VisionDataSocketClient();

        return s_instance;
    }

    public VisionDataSocketClient() {

        super("VisionDataSocketClient");
    }

    @Override protected void afterInit() {}

    @Override
    protected void afterUpdate() {

        switch (m_threadState) {

            case RUNNING: {

                switch (m_socketState) {

                    case OPEN: {
                        writeVisionDataToSocket();
                        break;
                    }
                }
            }
        }
    }

    /**
     * Writes the vision data (x and y distances) to the socket using an output stream.
     */
    private void writeVisionDataToSocket() {

        try {

            OutputStream out = m_client.getOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(out);

            JSONObject jsonObject = VisionInfoData.getJsonRepresentation();

            if (jsonObject != null) {

                String json = jsonObject.toString();

                writer.write(json);
            }

        } catch (IOException e) {

            e.printStackTrace();
        }
    }
}
