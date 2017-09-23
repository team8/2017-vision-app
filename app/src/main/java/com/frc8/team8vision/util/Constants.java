package com.frc8.team8vision.util;

import com.frc8.team8vision.R;

import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point3;

/**
 * Stores various constants to be used by the rest of the app.
 *
 * @author Calvin Yan
 */
public class Constants {

    // Universal logging tag
    public static final String kTAG = "FRC8.";

    // roboRIO networking constants
    public static final String kRIOHostName = "localhost";
    public static final int
        kDataPortNumber = 8008,
        kVisionPortNumber = 8009;
    public static final long
        kDataUpdateRateMS = 5,
        kVisionUpdateRateMS = 10,
        kChangeStateWaitMS = 200,
        kVisionIdleTimeS = 5;

    // HSV threshold slider constants
    public static final int[]
        kSliderIds = {R.id.hLow, R.id.sLow, R.id.vLow, R.id.hHigh, R.id.sHigh, R.id.vHigh},
        kSliderReadoutIds = {R.id.hLowInfo, R.id.sLowInfo, R.id.vLowInfo, R.id.hHighInfo, R.id.sHighInfo, R.id.vHighInfo},
        kSliderDefaultValues = {0, 0, 0, 180, 255, 255};
    public static final String[] kSliderNames = {"Minimum Hue", "Minimum Saturation", "Minimum Value",
                                                 "Maximum Hue", "Maximum Saturation", "Maximum Value"};

    // Setting option names
    public static final String
        kProfileSelection = "ProfileSelection",
        kProfileName = "Profile_Name",
        kTrackingLeft = "Tracking_Left",
        kDynamicTracking = "DynamicTracking",
        kFlashlightOn = "Flashlight_On",
        kTuningMode = "Tuning_Mode",
        kXShift = "X_Shift",
        kZShift = "Z_Shift",
        kTargetMode = "TargetMode",
        kProcessorMode = "ProcessorMode",
        kProcessorType = "ProcessorType";

    // Physical specs of peg (all measurements are in inches)
    public static final double kVisionTargetWidth = 10.25, kTapeWidth = 2, kVisionTargetHeight = 5.0, kPegLength = 10.5;

    // Source points
    public static final MatOfPoint3f kLeftSourcePoints = new MatOfPoint3f(
        new Point3(-kVisionTargetWidth/2           ,  kVisionTargetHeight/2, 0),
        new Point3(-kVisionTargetWidth/2+kTapeWidth,  kVisionTargetHeight/2, 0),
        new Point3(-kVisionTargetWidth/2           , -kVisionTargetHeight/2, 0),
        new Point3(-kVisionTargetWidth/2+kTapeWidth, -kVisionTargetHeight/2, 0)
    );
    public static final MatOfPoint3f kRightSourcePoints = new MatOfPoint3f(
        new Point3(kVisionTargetWidth/2-kTapeWidth,  kVisionTargetHeight/2, 0),
        new Point3(kVisionTargetWidth/2           ,  kVisionTargetHeight/2, 0),
        new Point3(kVisionTargetWidth/2-kTapeWidth, -kVisionTargetHeight/2, 0),
        new Point3(kVisionTargetWidth/2           , -kVisionTargetHeight/2, 0)
    );

    public static final MatOfPoint3f kAllSourcePoints = new MatOfPoint3f(
        VisionUtil.concat(kLeftSourcePoints.toArray(), kRightSourcePoints.toArray())
    );

    // Camera calibration constants

    // Galaxy S4
    public static final int kGalaxyPixelsPerInch = 441;
    public static final double kGalaxyFocalLengthX = 6513.75410, kGalaxyFocalLengthY = 6448.76817, kGalaxyFocalLengthZ = 527;
    public static final double[][] kGalaxyIntrinsicMatrix = {{kGalaxyFocalLengthX, 0,                   0},
                                                             {0,                   kGalaxyFocalLengthY, 0},
                                                             {0,                   0,                   1}};
    public static final double[] kGalaxyDistortionCoefficients = {.462497044, -1.63724827, -.00256097258, .00220231323};

    // Nexus 5x
    //public static final int kNexusPixelsPerInch = 424;
	public static final int kNexusPixelsPerInch = 180;
    //public static final int kNexusPixelsPerInch = 10770;
    public static final double kNexusFocalLengthX = 2004.00956, kNexusFocalLengthY = 2000.42830;
    public static final double[][] kNexusIntrinsicMatrix = {{kNexusFocalLengthX, 0,                  0},
                                                            {0,                  kNexusFocalLengthY, 0},
                                                            {0,                  0,                  1}};
    public static final double[] kNexusDistortionCoefficients = {.118331802, -.566093895, .000616728302, -.000492778707};

}
