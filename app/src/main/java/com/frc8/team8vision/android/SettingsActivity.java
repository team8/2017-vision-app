package com.frc8.team8vision.android;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Switch;

import com.frc8.team8vision.menu.StoredDoubleEntry;
import com.frc8.team8vision.util.Constants;
import com.frc8.team8vision.menu.HSVSeekBar;
import com.frc8.team8vision.R;

/**
 * This activity represents a setting screen that can be accessed to adjust
 * the HSV threshold. It maintains six sliders, or seekbars, which represent
 * the minimum and maximum HSV.
 *
 * @author Calvin Yan
 */
public class SettingsActivity extends AppCompatActivity {

    private HSVSeekBar[] seekBars = new HSVSeekBar[6];
	private static StoredDoubleEntry xShiftEntry = null, zShiftEntry = null;

    private static boolean trackingLeft, tuningMode, flashlightOn = false;

    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        trackingLeft = preferences.getBoolean("Tracking Left", true);

        RadioButton toCheck = (RadioButton)findViewById(((trackingLeft) ? R.id.target_left : R.id.target_right));
        if (tuningMode) toCheck = (RadioButton)findViewById(R.id.tuning_mode);
        toCheck.setChecked(true);

        ((Switch)findViewById(R.id.flashlight)).setChecked(flashlightOn);

        xShiftEntry = new StoredDoubleEntry(R.id.nexusXShift, "X_Shift", 0.0f, this);
        zShiftEntry = new StoredDoubleEntry(R.id.nexusZShift, "Z_Shift", 0.0f, this);

        for (int i = 0; i < 6; i++) {
            seekBars[i] = new HSVSeekBar(Constants.kSliderIds[i], Constants.kSliderReadoutIds[i],
                                         Constants.kSliderDefaultValues[i], Constants.kSliderNames[i], this);
        }
    }

    public void onRadioButtonClicked(View view) {
        SharedPreferences.Editor editor = preferences.edit();
        trackingLeft = view.getId() == R.id.target_left;
        tuningMode = view.getId() == R.id.tuning_mode;
        editor.putBoolean("Tracking Left", trackingLeft);
        editor.apply();
    }

    public void onSwitchClicked(View view) {
        SharedPreferences.Editor editor = preferences.edit();
        flashlightOn = !flashlightOn;
        Log.d("SettingsActivity", "SETTING VALUE TO " + flashlightOn);
        editor.putBoolean("Flashlight On", flashlightOn);
        editor.apply();
    }

    public static boolean trackingLeftTarget() { return trackingLeft; }

    public static boolean flashlightOn() { return flashlightOn; }

	public static void setTrackingLeft(boolean isTrackingLeft) {trackingLeft = isTrackingLeft;}

    public static boolean tuningMode() { return tuningMode;}

    public static void setFlashlightOn(boolean value) { flashlightOn = value; }

	public static double getNexusXShift() {
		if(xShiftEntry != null){
            return xShiftEntry.getValue();
        } else {
            return 0;
        }
	}
	public static double getNexusZShift() {
		if(zShiftEntry != null){
            return zShiftEntry.getValue();
        } else {
            return 0;
        }
	}

}