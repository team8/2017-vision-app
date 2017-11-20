package com.frc8.team8vision.vision.processors;

import com.frc8.team8vision.android.CameraInfo;
import com.frc8.team8vision.util.VisionPreferences;
import com.frc8.team8vision.util.AreaComparator;
import com.frc8.team8vision.util.Constants.Constants;
import com.frc8.team8vision.util.VisionUtil;
import com.frc8.team8vision.vision.VisionDataUnit;
import com.frc8.team8vision.vision.VisionProcessorBase;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Vision processor which takes into account both tape targets.
 *
 * @author Quintin
 */
public class DoubleTargetProcessor extends VisionProcessorBase {

    private final MatOfPoint3f kAllTargetMatrix;
    private final int kXPointShift;

    public DoubleTargetProcessor() {
        kAllTargetMatrix = new MatOfPoint3f(VisionUtil.concat(Constants.kLeftSourcePoints, Constants.kRightSourcePoints));
        kXPointShift = CameraInfo.Width()/2;
    }

    @Override
    public MatOfPoint[] getBestContours(ArrayList<MatOfPoint> contours, Mat input) {

        if (contours.size() >= 2) {

            // Sort contours in decreasing order of area
            Collections.sort(contours, new AreaComparator());

            // Get whether or not the biggest contour is the left or right tape target
            final boolean firstIsLeft = contours.get(0).toArray()[0].x < contours.get(1).toArray()[0].x;
            // Get the correct left and right contours
            final MatOfPoint
                left  = firstIsLeft ? contours.get(0) : contours.get(1),
                right = firstIsLeft ? contours.get(1) : contours.get(0);

            // Draw tape contours on screen
            Imgproc.drawContours(input, contours, 0, new Scalar(255, 0, 0));
            Imgproc.drawContours(input, contours, 1, new Scalar(0, 255, 0));

            // Return first two contours which should be the biggest
            return new MatOfPoint[] { left, right };

        }

        return null;
    }

    @Override
    public VisionDataUnit[] processContours(MatOfPoint[] bestContours, Mat input) {

        if (bestContours != null && bestContours.length == 2) {

            // Get corners for both targets
            Point[][] tapeCornersFromImage = new Point[2][4];
            for (int i = 0; i < 2; i++) tapeCornersFromImage[i] = VisionUtil.getCorners(bestContours[i], kXPointShift);


            // Combine into single array
            final Point[] allCorners = VisionUtil.concat(tapeCornersFromImage[0], tapeCornersFromImage[1]);

            final Point3 posePnP = VisionUtil.getPosePnP(kAllTargetMatrix, allCorners, input);
            output_data[IDX_OUT_ZDIST].set(posePnP.z + VisionPreferences.getZ_shift());
            output_data[IDX_OUT_XDIST].set(posePnP.x + VisionPreferences.getX_shift());
        } else {
            output_data[IDX_OUT_XDIST].setToDefault();
            output_data[IDX_OUT_ZDIST].setToDefault();
        }
        return output_data;
    }


}
