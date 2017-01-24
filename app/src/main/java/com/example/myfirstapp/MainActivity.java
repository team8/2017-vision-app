package com.example.myfirstapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "MainActivity";

    private static Mat inputHSV;

    private static Toast toast;

    private SketchyCameraView mCameraView;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV load success");
                    inputHSV = new Mat();
                    mCameraView.enableView();
                    mCameraView.toggleFlashLight();
                } break;
                default: {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCameraView = new SketchyCameraView(this, -1);
        setContentView(mCameraView);
        mCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mCameraView != null) {
            mCameraView.disableView();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();

        if (mCameraView != null) {
            mCameraView.disableView();
            if (inputHSV != null) inputHSV.release();

        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {}

    @Override
    public void onCameraViewStopped() {}

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat input = inputFrame.rgba();

        // Debug statement

        input = track(input);
        inputHSV.release();
        return input;
    }

    public Mat track(Mat input) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        int hLow = preferences.getInt("Minimum Hue", 0);
        int sLow = preferences.getInt("Minimum Saturation", 0);
        int vLow = preferences.getInt("Minimum Value", 0);
        int hHigh = preferences.getInt("Maximum Hue", 180);
        int sHigh = preferences.getInt("Maximum Saturation", 255);
        int vHigh = preferences.getInt("Maximum Value", 255);

        // Apply HSV thresholding to input
        Mat mask = new Mat();

        // Lower and upper hsv thresholds
        Scalar lower_bound = new Scalar(hLow, sLow, vLow), upper_bound = new Scalar(hHigh, sHigh, vHigh);

        Imgproc.cvtColor(input, inputHSV, Imgproc.COLOR_RGB2HSV);
        Core.inRange(inputHSV, lower_bound, upper_bound, mask);

        //Debug statement
        //Log.d(TAG, inputHSV.size().toString());

        // Identify and draw largest contour
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(mask, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        double[] maxes = {-1, -1};
        int[] maxIds = {-1, -1};
        for (int i = 0; i < contours.size(); i++) {
            double area = Imgproc.contourArea(contours.get(i));
            if (area > maxes[0]) {
                maxes[0] = area;
                maxIds[0] = i;
            } else if (area > maxes[1]) {
                maxes[1] = area;
                maxIds[1] = i;
            }
        }
        Imgproc.drawContours(input, contours, maxIds[0], new Scalar(0, 255, 0), 1);
        Imgproc.drawContours(input, contours, maxIds[1], new Scalar(255, 0, 0), 1);

        return input;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (toast != null) {
                toast.cancel();
            }
            //int x = dpToPx(event.getX()), y = dpToPx(event.getY());
            int x = (int)event.getX(), y = (int)event.getY();
            double[] hsv = inputHSV.get(y, (int)mCameraView.getX() - x);
            String message = Arrays.toString(hsv);
            toast = Toast.makeText(getApplicationContext(), mCameraView.getX() + " " + x, Toast.LENGTH_SHORT);
            toast.show();
        }

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    public void launchSetThresholdActivity(MenuItem item) {
        Intent intent = new Intent(this, SetThresholdActivity.class);
        startActivity(intent);
    }

    public int dpToPx(float dp) {
        DisplayMetrics displayMetrics = getBaseContext().getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

}
