package com.example.myfirstapp;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.SeekBar;
import android.widget.TextView;

public class HSVSeekBar implements SeekBar.OnSeekBarChangeListener{

    private Activity activity;

    private SeekBar slider;
    private TextView display;

    private int sliderId, displayId;

    private String title;

    private SharedPreferences preferences;

    public HSVSeekBar(int sId, int dId, int def, String title, Activity activity) {
        sliderId = sId;
        displayId = dId;
        this.title = title;
        this.activity = activity;

        slider = (SeekBar)activity.findViewById(sliderId);
        display = (TextView)activity.findViewById(displayId);

        preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        setProgress(preferences.getInt(title, def));
    }

    private void setProgress(int progress) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(title, progress);
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
