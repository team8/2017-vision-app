package com.frc8.team8vision.networking;

import com.frc8.team8vision.vision.VisionInfoData;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * Sends vision information (x and z distance) through a socket to the RoboRIO.
 */
public class VisionDataClient extends AbstractVisionClient {

    public VisionDataClient() {

        super("VisionDataClient");
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

            String json = VisionInfoData.getJsonRepresentation().toString();

            writer.write(json);

        } catch (IOException e) {

            e.printStackTrace();
        }
    }
}
