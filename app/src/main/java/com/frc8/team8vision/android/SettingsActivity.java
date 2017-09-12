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

import com.frc8.team8vision.menu.SelectionDropdown;
import com.frc8.team8vision.menu.StoredDoubleEntry;
import com.frc8.team8vision.util.Constants;
import com.frc8.team8vision.menu.HSVSeekBar;
import com.frc8.team8vision.R;
import com.frc8.team8vision.util.OnSelectionChangedCallback;
import com.frc8.team8vision.vision.ProcessorSelector;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * This activity represents a setting screen that can be accessed to adjust
 * the HSV threshold. It maintains six sliders, or seekbars, which represent
 * the minimum and maximum HSV.
 *
 * @author Calvin Yan
 */
public class SettingsActivity extends AppCompatActivity {
    public enum TargetMode{
        RIGHT_TARGET, LEFT_TARGET, TUNING
    }

    private HSVSeekBar[] seekBars = new HSVSeekBar[6];
	private static StoredDoubleEntry xShiftEntry = null, zShiftEntry = null;
    private static SelectionDropdown targetMode = null, processorMode = null, profileMode = null;
    private static String profile;

    private static boolean trackingLeft, tuningMode, flashlightOn = false;

    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        profile = preferences.getString(Constants.kProfileName, "Default");
        profileMode = new SelectionDropdown(R.id.profileSelection, Constants.kProfileSelection,
                new ArrayList<String>(Arrays.asList(profile)), this, true, new OnSelectionChangedCallback() {
            @Override
            public void selectionChanged(String label) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(Constants.kProfileName, label);
                editor.apply();
                initialize(label);
            }
        });

		Button deleteProfile = ((Button)findViewById(R.id.deleteProfile));
		deleteProfile.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String newProfile = profileMode.removeElement(profile);
				initialize(newProfile);
			}
		});

        initialize(profile);
    }

    private void initialize(final String profileIn){
        profile = profileIn;
        trackingLeft = preferences.getBoolean(profile+"_" + Constants.kTrackingLeft, true);
        flashlightOn = preferences.getBoolean(profile+"_" + Constants.kFlashlightOn, false);
		tuningMode = preferences.getBoolean(profile+"_" + Constants.kTuningMode, false);

        ((Switch)findViewById(R.id.flashlight)).setChecked(flashlightOn);

		if(xShiftEntry!=null) xShiftEntry.reset();
		if(zShiftEntry!=null) zShiftEntry.reset();

        xShiftEntry = new StoredDoubleEntry(R.id.nexusXShift, profile+"_" + Constants.kXShift, 0.0f, this);
        zShiftEntry = new StoredDoubleEntry(R.id.nexusZShift, profile+"_" + Constants.kZShift, 0.0f, this);

        targetMode = new SelectionDropdown(R.id.targetModeSelection, profile + "_" + Constants.kTargetMode,
				TargetMode.class, this, false, new OnSelectionChangedCallback() {
            @Override
            public void selectionChanged(String label) {
                SharedPreferences.Editor editor = preferences.edit();
                TargetMode mode = TargetMode.valueOf(label.toUpperCase());
                switch (mode){
                    case LEFT_TARGET:
                        tuningMode = false;
                        trackingLeft = true;
                        break;
                    case RIGHT_TARGET:
                        tuningMode = false;
                        trackingLeft = false;
                        break;
                    case TUNING:
                        tuningMode = true;
                        break;
                }
                editor.putBoolean(profile+"_" + Constants.kTrackingLeft, trackingLeft);
				editor.putBoolean(profile+"_" + Constants.kTuningMode, tuningMode);
				editor.apply();
            }
        });

		processorMode = new SelectionDropdown(R.id.processorSelection, profile + "_" + Constants.kProcessorMode,
				ProcessorSelector.ProcessorType.class, this, false, new OnSelectionChangedCallback() {
			@Override
			public void selectionChanged(String label) {
				SharedPreferences.Editor editor = preferences.edit();
				editor.putString(profile+"_" + Constants.kProcessorType, label.toUpperCase());
				editor.apply();
			}
		});

        for (int i = 0; i < 6; i++) {
			if(seekBars[i] == null){
				seekBars[i] = new HSVSeekBar(Constants.kSliderIds[i], Constants.kSliderReadoutIds[i],
						Constants.kSliderDefaultValues[i], Constants.kSliderNames[i], profile, this);
			} else {
				seekBars[i].initProfiles(profile, Constants.kSliderDefaultValues[i]);
			}
        }
    }

    public void onSwitchClicked(View view) {
        SharedPreferences.Editor editor = preferences.edit();
        flashlightOn = !flashlightOn;
        Log.d("SettingsActivity", "SETTING VALUE TO " + flashlightOn);
        editor.putBoolean(profile+"_" + Constants.kFlashlightOn, flashlightOn);
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

	public static String getProfile(){
		return profile;
	}

}