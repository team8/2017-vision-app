package com.example.myfirstapp;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;

import org.opencv.android.JavaCameraView;

public class SketchyCameraView extends JavaCameraView {

    public SketchyCameraView(Context context, int id) {
        super(context, id);
    }

    public void toggleFlashLight() {
        if (mCamera == null) return;
        Camera.Parameters param = mCamera.getParameters();
        param.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
        mCamera.setParameters(param);
    }



}
