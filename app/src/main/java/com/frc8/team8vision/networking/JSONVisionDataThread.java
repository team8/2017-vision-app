package com.frc8.team8vision.networking;

import android.app.Activity;
import android.util.Log;

import com.frc8.team8vision.util.Constants;
import com.frc8.team8vision.vision.VisionInfoData;


import org.json.JSONObject;

/**
 * Writes image data to JSON to be read by the RoboRIO
 *
 * @author Quintin Dwight
 */
public class JSONVisionDataThread extends AbstractJSONWriter {

    // Instance and state variables
    public static JSONVisionDataThread s_instance;
    private Activity m_activity;

    /**
     * Creates a instance of this
     * Cannot be called outside as a Singleton
     */
    private JSONVisionDataThread() {
        super("JSONVisionDataThread", "data.json");
    }

    /**
     * @return The instance of the singleton
     */
    public static JSONVisionDataThread getInstance() {

        if (s_instance == null)
            s_instance = new JSONVisionDataThread();

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

        final JSONObject json = VisionInfoData.getJsonRepresentation();

        return json;
    }

//    /**
//     * Converts Matrix representing image data to byte array
//     * @param image Image data
//     * @return Computed byte array
//     */
//    private byte[] toByteArray(Mat image) {
//        byte[] retval = new byte[image.rows() * image.cols() * image.channels() * 8];
//        int index = 0;
//        for (int i = 0; i < image.rows(); i++) {
//            for (int j = 0; j < image.cols(); j++) {
//                double[] pixel = image.get(i, j);
//                for (int k = 0; k < image.channels(); k++) {
//                    byte[] bytes = new byte[8];
//                    ByteBuffer.wrap(bytes).putDouble(pixel[k]);
//                    for (byte b : bytes) retval[index++] = b;
//                }
//            }
//        }
//        return retval;
//    }
}
