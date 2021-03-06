package com.frc8.team8vision.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.frc8.team8vision.vision.ProcessorSelector;

public class VisionPreferences {

	private static SharedPreferences preferences = null;
	private static Context context = null;

	private static String profile = "Default";
	private static boolean trackingLeft = false;
	private static boolean dynamicTracking = false;
	private static boolean tuningMode = false;
	private static boolean flashlightOn = false;
	private static float x_shift = 0.0f;
	private static float z_shift = 0.0f;

	private static int[] sliderValues = new int[6];
	private static ProcessorSelector.ProcessorType processorType = ProcessorSelector.ProcessorType.CENTROID;

	public static void initialize(Activity activity){
		preferences = PreferenceManager.getDefaultSharedPreferences(activity);
		context = activity.getBaseContext();
	}

	public static void updateSettings(){

		profile = preferences.getString(Constants.kProfileNameSettingsName, profile);
		trackingLeft = preferences.getBoolean(profile+"_"+Constants.kTrackingLeftSettingsName, trackingLeft);
		dynamicTracking = preferences.getBoolean(profile+"_"+Constants.kDynamicTrackingSettingsName, dynamicTracking);
		tuningMode = preferences.getBoolean(profile+"_"+ Constants.kTuningModeSettingsName, tuningMode);
		flashlightOn = preferences.getBoolean(profile+"_"+Constants.kFlashlightOnSettingsName, flashlightOn);
		x_shift = preferences.getFloat(profile+"_"+Constants.kXShiftSettingsName, x_shift);
		z_shift = preferences.getFloat(profile+"_"+Constants.kZShiftSettingsName, z_shift);

		String processorName = preferences.getString(profile+"_"+Constants.kProcessorTypeSettingsName, "CENTROID");
		processorType = ProcessorSelector.ProcessorType.valueOf(processorName);

		for(int i=0; i<sliderValues.length; i++){
			sliderValues[i] = preferences.getInt(profile+"_"+Constants.kSliderNames[i], Constants.kSliderDefaultValues[i]);
		}
	}

	public static Context context(){return context;}
	public static SharedPreferences preferences(){return preferences;}
	public static SharedPreferences.Editor editor(){return preferences.edit();}

	public static String getProfile(){return profile;}
	public static boolean isTrackingLeft() {return trackingLeft;}
	public static boolean isDynamicTracking() {return dynamicTracking;}
	public static boolean isTuningMode() {return tuningMode;}
	public static boolean isFlashlightOn() {return flashlightOn;}
	public static float getX_shift() {return x_shift;}
	public static float getZ_shift() {return z_shift;}
	public static ProcessorSelector.ProcessorType getProcessorType() {return processorType;}
	public static int[] getSliderValues(){return sliderValues;}

	public static void setTrackingLeft(boolean isTrackingLeft){
		trackingLeft = isTrackingLeft;
		editor().putBoolean(profile+"_"+ Constants.kTrackingLeftSettingsName, trackingLeft);
		editor().apply();
	}
	public static void setFlashlight(boolean isFlashlightOn){
		flashlightOn = isFlashlightOn;
		editor().putBoolean(profile+"_"+Constants.kFlashlightOnSettingsName, isFlashlightOn);
		editor().apply();
	}
}
