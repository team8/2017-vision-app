package com.example.myfirstapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Locale;

public class SetThresholdActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener{

    private SharedPreferences preferences;

    private SeekBar hLow, sLow, vLow, hHigh, sHigh, vHigh;

    private TextView hLowInfo, sLowInfo, vLowInfo, hHighInfo, sHighInfo, vHighInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_threshold);

        hLow = (SeekBar)findViewById(R.id.hLow);
        sLow = (SeekBar)findViewById(R.id.sLow);
        vLow = (SeekBar)findViewById(R.id.vLow);
        hHigh = (SeekBar)findViewById(R.id.hHigh);
        sHigh = (SeekBar)findViewById(R.id.sHigh);
        vHigh = (SeekBar)findViewById(R.id.vHigh);

        hLowInfo = (TextView)findViewById(R.id.hLowInfo);
        sLowInfo = (TextView)findViewById(R.id.sLowInfo);
        vLowInfo = (TextView)findViewById(R.id.vLowInfo);
        hHighInfo = (TextView)findViewById(R.id.hHighInfo);
        sHighInfo = (TextView)findViewById(R.id.sHighInfo);
        vHighInfo = (TextView)findViewById(R.id.vHighInfo);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        int[] values = new int[6];

        values[0] = preferences.getInt("Minimum Hue", 0);
        values[1] = preferences.getInt("Minimum Saturation", 0);
        values[2] = preferences.getInt("Minimum Value", 0);
        values[3] = preferences.getInt("Maximum Hue", 180);
        values[4] = preferences.getInt("Maximum Saturation", 255);
        values[5] = preferences.getInt("Maximum Value", 255);

        hLow.setProgress(values[0]);
        sLow.setProgress(values[1]);
        vLow.setProgress(values[2]);
        hHigh.setProgress(values[3]);
        sHigh.setProgress(values[4]);
        vHigh.setProgress(values[5]);

        hLowInfo.setText(String.format(Locale.getDefault(), "Minimum Hue: %d", values[0]));
        sLowInfo.setText(String.format(Locale.getDefault(), "Minimum Saturation: %d", values[1]));
        vLowInfo.setText(String.format(Locale.getDefault(), "Minimum Value: %d", values[2]));
        hHighInfo.setText(String.format(Locale.getDefault(), "Maximum Hue: %d", values[3]));
        sHighInfo.setText(String.format(Locale.getDefault(), "Maximum Saturation: %d", values[4]));
        vHighInfo.setText(String.format(Locale.getDefault(), "Maximum Value: %d", values[5]));

        hLow.setOnSeekBarChangeListener(this);
        sLow.setOnSeekBarChangeListener(this);
        vLow.setOnSeekBarChangeListener(this);
        hHigh.setOnSeekBarChangeListener(this);
        sHigh.setOnSeekBarChangeListener(this);
        vHigh.setOnSeekBarChangeListener(this);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        SharedPreferences.Editor editor = preferences.edit();

        switch(seekBar.getId()) {

            case (R.id.hLow):
                hLowInfo.setText(String.format(Locale.getDefault(), "Minimum Hue: %d", progress));
                editor.putInt("Minimum Hue", progress);
                break;
            case (R.id.sLow):
                sLowInfo.setText(String.format(Locale.getDefault(), "Minimum Saturation: %d", progress));
                editor.putInt("Minimum Saturation", progress);
                break;
            case (R.id.vLow):
                vLowInfo.setText(String.format(Locale.getDefault(), "Minimum Value: %d", progress));
                editor.putInt("Minimum Value", progress);
                break;
            case (R.id.hHigh):
                hHighInfo.setText(String.format(Locale.getDefault(), "Maximum Hue: %d", progress));
                editor.putInt("Maximum Hue", progress);
                break;
            case (R.id.sHigh):
                sHighInfo.setText(String.format(Locale.getDefault(), "Maximum Saturation: %d", progress));
                editor.putInt("Maximum Saturation", progress);
                break;
            case (R.id.vHigh):
                vHighInfo.setText(String.format(Locale.getDefault(), "Maximum Value: %d", progress));
                editor.putInt("Maximum Value", progress);
                break;
        }

        editor.apply();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}
}
