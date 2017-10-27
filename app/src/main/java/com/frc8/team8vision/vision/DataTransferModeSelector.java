package com.frc8.team8vision.vision;

import android.app.Activity;

import com.frc8.team8vision.networking.AbstractVisionThread;
import com.frc8.team8vision.networking.JSONVideoThread;
import com.frc8.team8vision.networking.JSONVisionDataThread;
import com.frc8.team8vision.networking.VideoSocketClient;
import com.frc8.team8vision.networking.VisionDataSocketClient;
import com.frc8.team8vision.util.Constants;

import java.util.HashMap;

/**
 * Selector for data transfer modes of both vision data and video.
 *
 * @author Quintin Dwight
 */
public class DataTransferModeSelector {

    public enum DataTransferMode {
        CAT_JSON, SOCKET
    }

    public static abstract class DataSenderTransferModeSelector {

        protected HashMap<DataTransferMode, AbstractVisionThread> transferers = new HashMap<>();

        protected DataTransferMode currentMode = DataTransferMode.CAT_JSON;

        public DataSenderTransferModeSelector(Activity activity, final boolean isTesting) {

            initMap(activity, isTesting);
        }

        /**
         * Initialize the hash map with the correct transfer classes
         *
         * @param activity The main activity
         * @param isTesting Whether or not we are testing, used for starting threads
         */
        protected abstract void initMap(Activity activity, final boolean isTesting);

        public void setTransfererMode(DataTransferMode newMode) {

            transferers.get(currentMode).pause();
            transferers.get(newMode).resume();

            currentMode = newMode;
        }

        public AbstractVisionThread getTransferer() {

            return transferers.get(currentMode);
        }

        public void stopAll() {

            for (AbstractVisionThread thread : transferers.values()) {
                thread.stop();
            }
        }
    }

    public static class VideoDataTransferModeSelector extends DataSenderTransferModeSelector {

        public VideoDataTransferModeSelector(Activity activity, final boolean isTesting) {

            super(activity, isTesting);
        }

        @Override
        protected void initMap(Activity activity, final boolean isTesting) {

            JSONVideoThread json = new JSONVideoThread();
            json.start(activity, Constants.kDataUpdateRateMS);
            json.pause();

            VideoSocketClient socket = new VideoSocketClient();
            socket.start(activity, Constants.kVisionUpdateRateMS, Constants.kRIOHostName, Constants.kVideoPort, isTesting);
            socket.pause();

            transferers.put(DataTransferMode.CAT_JSON, json  );
            transferers.put(DataTransferMode.SOCKET  , socket);
        }
    }

    public static class VisionDataTransferModeSelector extends DataSenderTransferModeSelector {

        public VisionDataTransferModeSelector(Activity activity, final boolean isTesting) {

            super(activity, isTesting);
        }

        @Override
        protected void initMap(Activity activity, final boolean isTesting) {

            JSONVisionDataThread json = new JSONVisionDataThread();
            json.start(activity, Constants.kDataUpdateRateMS);
            json.pause();

            VisionDataSocketClient socket = new VisionDataSocketClient();
            socket.start(activity, Constants.kDataUpdateRateMS, Constants.kRIOHostName, Constants.kVisionDataPort, isTesting);
            socket.pause();

            transferers.put(DataTransferMode.CAT_JSON, json  );
            transferers.put(DataTransferMode.SOCKET  , socket);
        }
    }
}
