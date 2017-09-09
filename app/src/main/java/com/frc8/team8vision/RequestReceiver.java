package com.frc8.team8vision;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.frc8.team8vision.networking.JSONVisionDataThread;

import org.opencv.core.Mat;

import java.nio.ByteBuffer;

/**
 * Utility class that receives adb broadcasts from the robot and calls the
 * appropriate functions in WriteDataThread.
 *
 * @author Calvin Yan
 */
public class RequestReceiver extends BroadcastReceiver {

    private static final String TAG = "RequestReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            if (intent.getStringExtra("type").equals("flash")) {
                // Toggle flashlight
                boolean isFlash = intent.getBooleanExtra("isFlash", true);
                MainActivity.toggleFlash(isFlash);
            }
//            Log.i("RequestReceiver", "Received request to send data");
            JSONVisionDataThread.getInstance().sendBroadcast();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private byte[] toByteArray(Mat image) {
        byte[] retval = new byte[image.rows() * image.cols() * image.channels() * 8];
        int index = 0;
        for (int i = 0; i < image.rows(); i++) {
            for (int j = 0; j < image.cols(); j++) {
                double[] pixel = image.get(i, j);
                for (int k = 0; k < image.channels(); k++) {
                    byte[] bytes = new byte[8];
                    ByteBuffer.wrap(bytes).putDouble(pixel[k]);
                    for (byte b : bytes) retval[index++] = b;
                }
            }
        }
        return retval;
    }

}
