package com.frc8.team8vision;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Toast;

import org.json.JSONException;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
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
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "MainActivity";

    private static Mat imageRGB;

    private static double turnAngle = 0, xDist = 0;
    private static long cycleTime = 0;

    private MatOfDouble distCoeffs;

    private Mat intrinsicMatrix, imageHSV;

    private SketchyCameraView mCameraView;

    private long lastCycleTimestamp = 0;

    private int mWidth = 0, mHeight = 0, mPPI = 0;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV load success");
                    imageHSV = new Mat();
                    mCameraView.enableView();

                    intrinsicMatrix = new Mat(3, 3, CvType.CV_64F);
                    distCoeffs = new MatOfDouble();

                    if (isGalaxy()) {
                        // Galaxy S4 is being used
                        for (int i = 0; i < 3; i++) {
                            for (int j = 0; j < 3; j++) {
                                intrinsicMatrix.put(i, j, Constants.kGalaxyIntrinsicMatrix[i][j]);
                            }
                        }
                        distCoeffs.fromArray(Constants.kGalaxyDistortionCoefficients);
                        mPPI = Constants.kGalaxyPixelsPerInch;
                    } else {
                        // Nexus 5x is being used
                        for (int i = 0; i < 3; i++) {
                            for (int j = 0; j < 3; j++) {
                                intrinsicMatrix.put(i, j, Constants.kNexusIntrinsicMatrix[i][j]);
                            }
                        }
                        distCoeffs.fromArray(Constants.kNexusDistortionCoefficients);
                        mPPI = Constants.kNexusPixelsPerInch;
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
        Log.i("TEST ALVIN", "Activity Created");

        mCameraView = new SketchyCameraView(this, -1);
        setContentView(mCameraView);
        mCameraView.setCvCameraViewListener(this);

        WriteDataThread.getInstance().start(this, WriteDataThread.WriteState.JSON);
    }

    @Override
    public void onPause() {
        WriteDataThread.getInstance().pause();
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
            if (imageHSV != null) imageHSV.release();
        }
        WriteDataThread.getInstance().destroy();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mWidth = width;
        mHeight = height;
        mCameraView.setParameters();
        mCameraView.toggleFlashLight();
        WriteDataThread.getInstance().resume();
    }

    @Override
    public void onCameraViewStopped() {
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        imageRGB = inputFrame.rgba();
        if (isGalaxy()) Core.flip(imageRGB, imageRGB, -1); // Necessary because Galaxy camera feed is inverted
        imageRGB = track(imageRGB);
        imageHSV.release();

        return imageRGB;
    }

    public Mat track(Mat input) {
        if (lastCycleTimestamp != 0) cycleTime = System.currentTimeMillis() - lastCycleTimestamp;
        lastCycleTimestamp = System.currentTimeMillis();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        int[] sliderValues = new int[6];

        for (int i = 0; i < 6; i++) {
            sliderValues[i] = preferences.getInt(Constants.kSliderNames[i], Constants.kSliderDefaultValues[i]);
        }

        // Apply HSV thresholding to input
        Mat mask = new Mat();

        // Lower and upper hsv thresholds
        Scalar lower_bound = new Scalar(sliderValues[0], sliderValues[1], sliderValues[2]),
                upper_bound = new Scalar(sliderValues[3], sliderValues[4], sliderValues[5]);

        Imgproc.cvtColor(input, imageHSV, Imgproc.COLOR_RGB2HSV);
        Core.inRange(imageHSV, lower_bound, upper_bound, mask);

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

        //Track corners of combined contour
        Point[] corners;
        if ((corners = getCorners(contours)) != null) {
            Scalar[] colors = {new Scalar(255, 0, 0), new Scalar(0, 255, 0),
                    new Scalar(0, 0, 255), new Scalar(0, 0, 0)};

            //corners = new Point[] {new Point(250, 750), new Point(1275, 750), new Point(250, 250), new Point(1275, 250)};

            for (int i = 0; i < 4; i++) {
                Imgproc.circle(input, corners[i], 15, colors[i], -1);
            }
            getPosePnP(corners, input);

            Imgproc.putText(input,
                    String.format(Locale.getDefault(), "%.2f",
                    xDist), new Point(0, mHeight - 30),
                    Core.FONT_HERSHEY_SIMPLEX, 2, new Scalar(0, 255, 0), 3);
        }

        Imgproc.drawContours(input, contours, -1, new Scalar(0, 255, 0), 2);

        Imgproc.putText(input,
                Double.toString(cycleTime), new Point(mWidth - 200, mHeight - 30),
                Core.FONT_HERSHEY_SIMPLEX, 2, new Scalar(0, 255, 0), 3);

        return input;
    }

    public Point[] getCorners(List<MatOfPoint> contours) {
        List<Point>  combined = new ArrayList<>();
        for (MatOfPoint contour : contours) {
            combined.addAll(contour.toList());
        }
        Point[] corners = new Point[4];
        Point[] array = combined.toArray(new Point[0]);
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
        corners[1] = array[0];
        corners[3] = array[array.length - 1];
        Arrays.sort(array, new Comparator<Point>() {
            @Override
            public int compare(Point o1, Point o2) {
                return (int)((o1.y + o1.x) - (o2.y + o2.x));
            }
        });
        corners[0] = array[0];
        corners[2] = array[array.length - 1];

        return corners;
    }

    public void getPosePnP(Point[] src, Mat input) {
        double dist = 0, scalar = 0.2, width = Constants.kVisionTargetWidth * mPPI * scalar, height = Constants.kVisionTargetHeight * mPPI * scalar;
        double leftX = 0, topY = 0, rightX = width, bottomY = height, z = dist - 2 * mPPI * scalar;

        Imgproc.rectangle(input, new Point(leftX, topY), new Point(rightX, bottomY), new Scalar(255, 255, 255));
        //src = new Point[]{new Point(0, 500), new Point(1025, 500), new Point(0, 0), new Point(1025, 0)};
        Scalar[] colors = {new Scalar(255, 0, 0), new Scalar(0, 255, 0),
                new Scalar(0, 0, 255), new Scalar(0, 0, 0)};
        MatOfPoint2f dstPoints = new MatOfPoint2f(src[0], src[1], src[2], src[3]);
        MatOfPoint3f srcPoints = new MatOfPoint3f(new Point3(leftX, topY, dist), new Point3(rightX, topY, dist),
                new Point3(rightX, bottomY, dist), new Point3(leftX, bottomY, dist));
        MatOfDouble rvecs = new MatOfDouble(), tvecs = new MatOfDouble();
        Calib3d.solvePnP(srcPoints, dstPoints, intrinsicMatrix, distCoeffs, rvecs, tvecs);
        MatOfPoint3f newPoints = new MatOfPoint3f(new Point3(leftX, topY, 0), new Point3(rightX, topY, 0), new Point3(rightX, bottomY, 0), new Point3(leftX, bottomY, 0),
                                                    new Point3(leftX, topY, z), new Point3(rightX, topY, z), new Point3(rightX, bottomY, z), new Point3(leftX, bottomY, z),
                                                    new Point3((leftX+rightX)/2, (topY+bottomY)/2, -1 * Constants.kPegLength * mPPI));
       /* MatOfPoint3f srcPoints = new MatOfPoint3f(new Point3(0, y, dist), new Point3(x, y, dist),
                new Point3(0, 0, dist), new Point3(x, 0, dist));
        MatOfDouble rvecs = new MatOfDouble(), tvecs = new MatOfDouble();
        Calib3d.solvePnP(srcPoints, dstPoints, intrinsicMatrix, distCoeffs, rvecs, tvecs);
        MatOfPoint3f newPoints = new MatOfPoint3f(new Point3(0, 0, 0), new Point3(x, 0, 0), new Point3(x, y, 0), new Point3(0, y, 0),
                new Point3(0, 0, z), new Point3(x, 0, z), new Point3(x, y, z), new Point3(0, y, z));*/
        MatOfPoint2f result = new MatOfPoint2f();
        Calib3d.projectPoints(newPoints, rvecs, tvecs, intrinsicMatrix, distCoeffs, result);
        Point[] arr = result.toArray();
        Scalar red = new Scalar(255, 0, 0);
        for (int i = 0; i < 4; i++) {
            Imgproc.line(input, arr[i], arr[(i+1) % 4], red, 5);
            Imgproc.line(input, arr[i], arr[i+4], red, 5);
            Imgproc.line(input, arr[i+4], arr[(i+1) % 4+4], new Scalar(0, 0, 255), 5);
        }
        Imgproc.circle(input, arr[8], 10, new Scalar(255, 255, 255), -1);
        Log.d(TAG, tvecs.size().toString());
        xDist = tvecs.get(0, 0)[0] / mPPI;
        /*Point3[] newSrc = new Point3[4];
        for (int i = 0; i < 4; i++) newSrc[i] = new Point3(src[i].x, src[i].y, 500);
        srcPoints = new MatOfPoint3f();
        srcPoints.fromArray(newSrc);
        dstPoints = new MatOfPoint2f(new Point(0, y), new Point(x, y), new Point(0, 0), new Point(x, 0));
        Calib3d.solvePnP(srcPoints, dstPoints, intrinsicMatrix, distCoeffs, rvecs, tvecs);*/
        double[] angles = rvecs.toArray();
        turnAngle = Math.toDegrees(angles[1]);
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

    private boolean isGalaxy() { return Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP; }

    public static Mat getImage() {
        if (imageRGB.empty()) {
            return null;
        }
        Mat resized_rgb = new Mat();
        Imgproc.resize(imageRGB, resized_rgb, new Size(320, 180));
        return resized_rgb;
    }

    public static double getTurnAngle() { return turnAngle; }

    public static double getXDisplacement() { return xDist; }

    public static long getCycleTime() { return cycleTime; }
}
