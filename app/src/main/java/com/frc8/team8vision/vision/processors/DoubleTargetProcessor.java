package com.frc8.team8vision.vision.processors;

import com.frc8.team8vision.android.SettingsActivity;
import com.frc8.team8vision.vision.VisionData;
import com.frc8.team8vision.vision.VisionProcessorBase;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.MatOfPoint;

import java.util.ArrayList;

import static com.frc8.team8vision.util.VisionUtil.bestContour;
import static com.frc8.team8vision.util.VisionUtil.getCorners;
import static com.frc8.team8vision.util.VisionUtil.getPosePnP;

/**
 * @author Quintin
 */

public class DoubleTargetProcessor extends VisionProcessorBase {

    @Override
    public VisionData[] process(Mat input, Mat mask) {
        // Find the peg target contour
        ArrayList<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(mask, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        if (contours.isEmpty()) {
            output_data[IDX_OUT_FUNCTION_EXECUTION_CODE].set(EXECUTION_CODE_FAIL);
            output_data[IDX_OUT_EXECUTION_MESSAGE].set("Empty set of contours");
            return output_data;
        }
        MatOfPoint contour = bestContour(contours, input);

        if (contour != null) {
            Point[] corners = getCorners(contour);
            double[] tvecs = getPosePnP(corners, input);
            output_data[IDX_OUT_ZDIST].set(tvecs[2] + SettingsActivity.getNexusZShift());
            output_data[IDX_OUT_XDIST].set(tvecs[0] + SettingsActivity.getNexusXShift());

        } else {
            output_data[IDX_OUT_XDIST].setToDefault();
            output_data[IDX_OUT_ZDIST].setToDefault();
        }

        return output_data;
    }
}
