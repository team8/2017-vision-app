package com.frc8.team8vision.util;

import org.opencv.core.MatOfPoint;
import org.opencv.imgproc.Imgproc;

import java.util.Comparator;

/**
 * @author QuintinDwight
 */
public class AreaComparator implements Comparator<MatOfPoint> {
    @Override
    public int compare(MatOfPoint one, MatOfPoint two) {
        return Double.compare(Imgproc.contourArea(two), Imgproc.contourArea(one));
    }
}
