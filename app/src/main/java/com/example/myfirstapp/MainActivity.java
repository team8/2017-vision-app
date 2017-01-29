package com.example.myfirstapp;

import android.app.Activity;
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
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "MainActivity";

    private static Mat intrinsicMatrix;

    private static final double focalLength = 620, x0 = 960, y0 = 540;

    // Samsung Galaxy S4
    private static final double[][] intrinsics = {{focalLength,             0,   x0},
            {0,    focalLength,   y0},
            {0,             0,            1}};

    private static double x = 0, y = 0, z = Math.PI/2;

    private static double[][] xArr = {{1, 0, 0}, {0, Math.cos(x), -Math.sin(x)}, {0, Math.sin(x), Math.cos(x)}},
            yArr = {{Math.cos(y), 0, Math.sin(y)}, {0, 1, 0}, {-Math.sin(y), 0, Math.cos(y)}},
            zArr = {{Math.cos(z), -Math.sin(z), 0}, {Math.sin(z), Math.cos(z), 0}, {0, 0, 1}};

    private static Mat xMat, yMat, zMat;

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
                    xMat = new Mat(3, 3, CvType.CV_64F);
                    yMat = new Mat(3, 3, CvType.CV_64F);
                    zMat = new Mat(3, 3, CvType.CV_64F);
                    mCameraView.enableView();
                    mCameraView.toggleFlashLight();

                    intrinsicMatrix = new Mat(3, 3, CvType.CV_64F);
                    for (int i = 0; i < 3; i++) {
                        for (int j = 0; j < 3; j++) {
                            intrinsicMatrix.put(i, j, intrinsics[i][j]);
                        }
                    }
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

        // Detect contours
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(mask, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        if (contours.isEmpty()) return input;

        // Sort contours by area and identify the two largest
        Collections.sort(contours, new Comparator<MatOfPoint>() {
            @Override
            public int compare(MatOfPoint m1, MatOfPoint m2) {
                double area1 = Imgproc.contourArea(m1), area2 = Imgproc.contourArea(m2);
                if (area1 > area2) return -1;
                if (area1 < area2) return 1;
                return 0;
            }
        });
        List<MatOfPoint> largestTwo = new ArrayList<>();
        largestTwo.add(contours.get(0));
        if (contours.size() > 1) largestTwo.add(contours.get(1));

        // Combine largest two contours
        MatOfPoint combined = new MatOfPoint();
        if (largestTwo.size() == 2) {
            MatOfPoint first = largestTwo.get(0), second = largestTwo.get(1);
            combined = concat(first, second);
        }

        //Track corners of combined contour
        Point[] corners;
        if ((corners = getCorners(combined)) != null) {
            Scalar[] colors = {new Scalar(255, 0, 0), new Scalar(0, 255, 0),
                    new Scalar(0, 0, 255), new Scalar(0, 0, 0)};

            for (int i = 0; i < 4; i++) {
                Imgproc.circle(input, corners[i], 15, colors[i], -1);
            }
            double[] yaw = getAnglePnP(corners, input);

            if (yaw != null) Imgproc.putText(input,
                    String.format(Locale.getDefault(), "%.2f %.2f %.2f %.2f",
                    yaw[0], yaw[1], yaw[2], yaw[3]), new Point(0, 700),
                    Core.FONT_HERSHEY_SIMPLEX, 2, new Scalar(0, 255, 0), 3);
        }

        Imgproc.drawContours(input, largestTwo, -1, new Scalar(0, 255, 0), 2);
        return input;
    }

    public MatOfPoint concat(MatOfPoint m1, MatOfPoint m2) {
        Point[] arr1 = m1.toArray(), arr2 = m2.toArray();
        Point[] combined = new Point[arr1.length + arr2.length];
        int i;
        for (i = 0; i < arr1.length; i++) {
            combined[i] = arr1[i];
        }
        for (int j = 0; j < arr2.length; j++, i++) {
            combined[i] = arr2[j];
        }
        MatOfPoint retval = new MatOfPoint();
        retval.fromArray(combined);
        return retval;
    }

    public Point[] getCorners(MatOfPoint contour) {
        if (contour == null) {
            Log.d(TAG, "Contour is null");
            return null;
        }
        Point[] corners = new Point[4];
        Point[] array = contour.toArray();
        if (array.length == 0) {
            Log.d(TAG, "Empty array");
            return null;
        }
        Arrays.sort(array, new Comparator<Point>() {
            @Override
            public int compare(Point o1, Point o2) {
                return (int)((o1.y - o1.x) - (o2.y - o2.x));
            }
        });
        corners[3] = array[0];
        corners[0] = array[array.length - 1];
        Arrays.sort(array, new Comparator<Point>() {
            @Override
            public int compare(Point o1, Point o2) {
                return (int)((o1.y + o1.x) - (o2.y + o2.x));
            }
        });
        corners[2] = array[0];
        corners[1] = array[array.length - 1];

        return corners;
    }

    public double[] getAngle(Point[] src) {
        Point[] dst = {new Point(0, 5), new Point (10.25, 5),
                        new Point(0, 0), new Point (10.25, 0)};
        MatOfPoint2f srcPoints = new MatOfPoint2f(), dstPoints = new MatOfPoint2f();
        srcPoints.fromArray(src);
        dstPoints.fromArray(dst);
        Mat homography = Calib3d.findHomography(srcPoints, dstPoints);
        List<Mat> rvecs = new ArrayList<>(), tvecs = new ArrayList<>(), normals = new ArrayList<>();
        if (!homography.isContinuous()) return null;
        Log.d(TAG, intrinsicMatrix.size().toString());
        Calib3d.decomposeHomographyMat(homography, intrinsicMatrix, rvecs, tvecs, normals);
        double[] angles = new double[4];
        for (int i = 0; i < 4; i++) {
            angles[i] = getYaw(rvecs.get(i));
        }
        Log.d(TAG, Arrays.toString(angles));
        return angles;
    }

    public double[] getAnglePnP(Point[] src, Mat input) {
        MatOfPoint2f srcPoints = new MatOfPoint2f(src[0], src[1], src[2], src[3]);
        MatOfPoint3f dstPoints = new MatOfPoint3f(new Point3(0, 5, 0), new Point3(10.25, 5, 0),
                new Point3(0, 0, 0), new Point3(10.25, 0, 0));
        Mat rvecs = new Mat(3, 3, CvType.CV_64F), tvecs = new Mat(3, 1, CvType.CV_64F);
        Calib3d.solvePnP(dstPoints, srcPoints, intrinsicMatrix, new MatOfDouble(Mat.zeros(4, 1, CvType.CV_64F)), rvecs, tvecs);
        Log.d(TAG, rvecs.size().toString());
        double[] angles = new double[4];
        if (rvecs == null) return null;
        Log.d(TAG, Arrays.toString(rvecs.get(0, 0)));
        double previous = 0;
        for (int i = 0; i < 3; i++) {
            if (rvecs.get(0, i) != null) {
                angles[i] = Math.toDegrees(rvecs.get(0, i)[0]);
                previous = angles[i];
            } else {
                angles[i] = previous;
            }

        }
        MatOfPoint3f newPoints = new MatOfPoint3f(new Point3(0, 0, 0), new Point3(3, 0, 0), new Point3(0, 3, 0), new Point3(0, 0, 3));
        MatOfPoint2f result = new MatOfPoint2f();
        //Log.d(TAG, Integer.toString(tvecs.rows()));
        Log.d(TAG, String.format("%.2f %.2f %.2f", tvecs.get(0, 0)[0], tvecs.get(1, 0)[0], tvecs.get(2, 0)[0]));
        Calib3d.projectPoints(newPoints, rvecs, tvecs, intrinsicMatrix, new MatOfDouble(0,0,0,0), result);
        Point[] arr = result.toArray();
        for (int i = 1; i < 4; i++) {
            Imgproc.line(input, arr[0], arr[i], new Scalar(0, 0, 255), 3);
        }

        return angles;
    }


    public double getYaw(Mat rotationMatrix) {
        double theta1, theta2, theta3, s1, c1, c2;
        theta1 = Math.atan2(rotationMatrix.get(1,2)[0], rotationMatrix.get(2,2)[0]);
        c2 = Math.sqrt(rotationMatrix.get(0,0)[0] * rotationMatrix.get(0,0)[0] + rotationMatrix.get(0,1)[0] * rotationMatrix.get(0,1)[0]);
        theta2 = Math.atan2(-rotationMatrix.get(0,2)[0], c2);
        s1 = Math.sin(theta1);
        c1 = Math.cos(theta1);
        theta3 = Math.atan2(s1 * rotationMatrix.get(2,0)[0] - c2 * rotationMatrix.get(1,0)[0], c1 * rotationMatrix.get(1,1)[0] - s1 * rotationMatrix.get(2,1)[0]);
        return Math.toDegrees(theta1);
    }

/*    public void verifyYaw() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                xMat.put(i, j, xArr[i][j]);
                yMat.put(i, j, yArr[i][j]);
                zMat.put(i, j, zArr[i][j]);
            }
        }
        Mat rotMatrix = new Mat(3, 3, CvType.CV_64F), temp = new Mat(3, 3, CvType.CV_64F);
        Log.d(TAG, String.format(Locale.getDefault(), "%s\n%s\n%s", zMat.get(0, 0, new double[0]), yMat, xMat));
        Core.multiply(zMat, yMat, temp);
        Core.multiply(temp, xMat, rotMatrix);
        Log.d(TAG, rotMatrix.toString());
        if (z != getYaw(rotMatrix)) Log.d(TAG, "You failed");
    }*/

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (toast != null) {
                toast.cancel();
            }
            //int x = dpToPx(event.getX()), y = dpToPx(event.getY());
            int row = getRow(event.getY()), col = getCol(event.getY());
            double[] hsv = inputHSV.get(row, col);
            String message = Arrays.toString(hsv);
            toast = Toast.makeText(getApplicationContext(), event.getX() + " " + event.getY(), Toast.LENGTH_SHORT);
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

    public int getRow(double y) {
        return (int)(y - 360);
    }

    public int getCol(double x) {
        return (int)(x - 320);
    }


    public int dpToPx(float dp) {
        DisplayMetrics displayMetrics = getBaseContext().getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

}
