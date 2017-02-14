package com.frc8.team8vision;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.opencv.core.Mat;

import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

public class RequestReceiver extends BroadcastReceiver {

    private static final String TAG = "RequestReceiver", hostName = "roboRIO-8-frc.local";

    private static final int RIOPort = 8008;
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            Socket socket = new Socket(hostName, RIOPort);
            OutputStream out = socket.getOutputStream();
            Mat image = MainActivity.getImage();
            byte[] byteArray = toByteArray(image);
            out.write(byteArray);
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
