package com.frc8.team8vision;

public class Constants {

    // roboRIO networking constants
    public static final String kRIOHostName = "roboRIO-8-frc.local";
    public static final int kRIOPortNumber = 8008;

    // HSV threshold slider constants
    public static final int[] kSliderIds = {R.id.hLow, R.id.sLow, R.id.vLow, R.id.hHigh, R.id.sHigh, R.id.vHigh},
                              kSliderReadoutIds = {R.id.hLowInfo, R.id.sLowInfo, R.id.vLowInfo, R.id.hHighInfo, R.id.sHighInfo, R.id.vHighInfo},
                              kSliderDefaultValues = {0, 0, 0, 180, 255, 255};
    public static final String[] kSliderNames = {"Minimum Hue", "Minimum Saturation", "Minimum Value",
                                                 "Maximum Hue", "Maximum Saturation", "Maximum Value"};

    // Physical specs of peg (all measurements are in inches)
    public static final double kVisionTargetWidth = 10.25, kTapeWidth = 2, kVisionTargetHeight = 5.0, kPegLength = 10.5;

    // Camera calibration constants

    // Galaxy S4
    public static final int kGalaxyPixelsPerInch = 441;
    public static final double kGalaxyFocalLengthX = 6513.75410, kGalaxyFocalLengthY = 6448.76817, kGalaxyFocalLengthZ = 527;
    public static final double[][] kGalaxyIntrinsicMatrix = {{kGalaxyFocalLengthX, 0,                   0},
                                                             {0,                   kGalaxyFocalLengthY, 0},
                                                             {0,                   0,                   1}};
    public static final double[] kGalaxyDistortionCoefficients = {.462497044, -1.63724827, -.00256097258, .00220231323};

    // Nexus 5x
    public static final int kNexusPixelsPerInch = 424;
    public static final double kNexusFocalLengthX = 2004.00956, kNexusFocalLengthY = 2000.42830;
    public static final double[][] kNexusIntrinsicMatrix = {{kNexusFocalLengthX, 0,                  0},
                                                            {0,                  kNexusFocalLengthY, 0},
                                                            {0,                  0,                  1}};
    public static final double[] kNexusDistortionCoefficients = {.118331802, -.566093895, .000616728302, -.000492778707};

}
