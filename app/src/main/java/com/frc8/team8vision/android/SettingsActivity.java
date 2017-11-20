package com.frc8.team8vision.android;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;

import com.frc8.team8vision.menu.SelectionDropdown;
import com.frc8.team8vision.menu.StoredDoubleEntry;
import com.frc8.team8vision.util.Constants.Constants;
import com.frc8.team8vision.menu.HSVSeekBar;
import com.frc8.team8vision.R;
import com.frc8.team8vision.util.OnSelectionChangedCallback;
import com.frc8.team8vision.util.VisionPreferences;
import com.frc8.team8vision.vision.ProcessorSelector;
import com.frc8.team8vision.vision.DataTransferModeSelector;

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

    public enum TargetMode {
        RIGHT_TARGET, LEFT_TARGET, DYNAMIC_TARGET, TUNING
    }

    private HSVSeekBar[] seekBars = new HSVSeekBar[6];
	private StoredDoubleEntry xShiftEntry = null, zShiftEntry = null;
    private SelectionDropdown
		targetMode = null, processorMode = null, profileMode = null, visionDataTransferMode = null, videoDataTransferMode;
    private String profile;

    private boolean trackingLeft, dynamicTrack, tuningMode, flashlightOn = false;

    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        preferences = VisionPreferences.preferences();

        profile = preferences.getString(Constants.kProfileNameSettingsName, "Default");
        profileMode = new SelectionDropdown(R.id.profileSelection, Constants.kProfileSelectionSettingsName,
			new ArrayList<>(Arrays.asList(profile)), this, true, new OnSelectionChangedCallback() {
				@Override
				public void selectionChanged(String label) {
					SharedPreferences.Editor editor = preferences.edit();
					editor.putString(Constants.kProfileNameSettingsName, label);
					editor.apply();
					loadSettings(label);
				}
        	}
        );
		profileMode.initProfiles(profile);

		targetMode = new SelectionDropdown(R.id.targetModeSelection, Constants.kTargetModeSettingsName,
			TargetMode.class, this, false, new OnSelectionChangedCallback() {
				@Override
				public void selectionChanged(String label) {
					SharedPreferences.Editor editor = preferences.edit();
					TargetMode mode = TargetMode.valueOf(label.toUpperCase());
					switch (mode) {
						case LEFT_TARGET:
							dynamicTrack = false;
							tuningMode = false;
							trackingLeft = true;
							break;
						case RIGHT_TARGET:
							dynamicTrack = false;
							tuningMode = false;
							trackingLeft = false;
							break;
						case DYNAMIC_TARGET:
							tuningMode = false;
							dynamicTrack = true;
							break;
						case TUNING:
							tuningMode = true;
							break;
					}
					editor.putBoolean(profile+"_" + Constants.kTrackingLeftSettingsName, trackingLeft);
					editor.putBoolean(profile+"_" + Constants.kDynamicTrackingSettingsName, dynamicTrack);
					editor.putBoolean(profile+"_" + Constants.kTuningModeSettingsName, tuningMode);
					editor.apply();
				}
			}
		);

		processorMode = new SelectionDropdown(R.id.processorSelection, Constants.kProcessorModeSettingsName,
			ProcessorSelector.ProcessorType.class, this, false, new OnSelectionChangedCallback() {
			@Override
			public void selectionChanged(String label) {
				SharedPreferences.Editor editor = preferences.edit();
				editor.putString(profile+"_" + Constants.kProcessorTypeSettingsName, label.toUpperCase());
				editor.apply();
			}
		});

		visionDataTransferMode = new SelectionDropdown(
				R.id.visionDataTransferModeSelection,
				Constants.kVisionDataTransferModeSettingsName,
				DataTransferModeSelector.DataTransferMode.class, this, false, new OnSelectionChangedCallback() {
				@Override
				public void selectionChanged(String label) {
					SharedPreferences.Editor editor = preferences.edit();
					editor.putString(profile+"_" + Constants.kVisionDataTransferModeSettingsName, label.toUpperCase());
					editor.apply();
				}
		});

		videoDataTransferMode = new SelectionDropdown(
				R.id.videoDataTransferModeSelection,
				Constants.kVideoDataTransferModeSettingsName,
				DataTransferModeSelector.DataTransferMode.class, this, false, new OnSelectionChangedCallback() {
				@Override
				public void selectionChanged(String label) {
					SharedPreferences.Editor editor = preferences.edit();
					editor.putString(profile+"_" + Constants.kVideoDataTransferModeSettingsName, label.toUpperCase());
					editor.apply();
				}
		});

		Button deleteProfile = ((Button)findViewById(R.id.deleteProfile));

		deleteProfile.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String newProfile = profileMode.removeElement(profile);
				loadSettings(newProfile);
			}
		});

		xShiftEntry = new StoredDoubleEntry(R.id.nexusXShift, Constants.kXShiftSettingsName, this);
		zShiftEntry = new StoredDoubleEntry(R.id.nexusZShift, Constants.kZShiftSettingsName, this);

		for (int i = 0; i < 6; i++) {
			seekBars[i] = new HSVSeekBar(
				Constants.kSliderIds[i], Constants.kSliderReadoutIds[i],
				Constants.kSliderNames[i], this
			);
		}

        loadSettings(profile);
    }

    private void loadSettings(final String profileIn) {

        profile = profileIn;
        trackingLeft = preferences.getBoolean(profile+"_" + Constants.kTrackingLeftSettingsName, true);
		dynamicTrack = preferences.getBoolean(profile+"_" + Constants.kDynamicTrackingSettingsName, true);
        flashlightOn = preferences.getBoolean(profile+"_" + Constants.kFlashlightOnSettingsName, false);
		tuningMode = preferences.getBoolean(profile+"_" + Constants.kTuningModeSettingsName, false);

        ((Switch)findViewById(R.id.flashlight)).setChecked(flashlightOn);

		xShiftEntry.initProfiles(profile, 0.0f);
		zShiftEntry.initProfiles(profile, 0.0f);

		targetMode.initProfiles(profile);
		processorMode.initProfiles(profile);
		visionDataTransferMode.initProfiles(profile);
		videoDataTransferMode.initProfiles(profile);

        for (int i = 0; i < 6; i++) {
			seekBars[i].initProfiles(profile, Constants.kSliderDefaultValues[i]);
        }
    }

    public void onSwitchClicked(View view) {

        SharedPreferences.Editor editor = preferences.edit();
        flashlightOn = !flashlightOn;
        Log.d("SettingsActivity", "SETTING VALUE TO " + flashlightOn);
        editor.putBoolean(profile+"_" + Constants.kFlashlightOnSettingsName, flashlightOn);
        editor.apply();
    }
}