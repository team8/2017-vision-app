package com.frc8.team8vision;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;

import org.opencv.android.JavaCameraView;

public class SketchyCameraView extends JavaCameraView {

    private static final String TAG = "SketchyCameraView";

    public SketchyCameraView(Context context, int id) {
        super(context, id);
    }

    public void toggleFlashLight() {
        if (mCamera == null) {
            Log.w(TAG, "Camera not instantiated");
            return;
        }
        Camera.Parameters param = mCamera.getParameters();
        param.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);

        param.setExposureCompensation(param.getMinExposureCompensation());
        mCamera.setParameters(param);
    }



}
