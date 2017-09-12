package com.frc8.team8vision.menu;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Custom seekbar class. An HSVSeekBar is an encapsulation of one of the seekbars
 * in the set threshold activity, and contains the seekbar itself, as well as its
 * name, id, and text field readout. This is mostly for easier retrieval of stored settings.
 *
 * @author Calvin Yan
 */
public class HSVSeekBar implements SeekBar.OnSeekBarChangeListener{

    private SeekBar slider;
    private TextView display;

    private int sliderId;

    private String title;
    private String name;

    // The settings contained in the app - similar to a hash table
    private SharedPreferences preferences;

    public HSVSeekBar(int sliderId, int displayId, String title, Activity activity) {
        this.sliderId = sliderId;
        this.title = title;

        slider = (SeekBar)activity.findViewById(sliderId);
        display = (TextView)activity.findViewById(displayId);

        preferences = PreferenceManager.getDefaultSharedPreferences(activity);

        slider.setOnSeekBarChangeListener(this);
    }

    public void initProfiles(String profile, int def){
		this.name = profile + "_" + title;
		setProgress(preferences.getInt(name, def));
	}

    // Change slider and store its value in preferences
    private void setProgress(int progress) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(name, progress);
        slider.setProgress(progress);
        display.setText(title + ": " + progress);
        editor.apply();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (seekBar.getId() == sliderId) setProgress(progress);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}
}
