package com.frc8.team8vision;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

/**
 * This activity represents a setting screen that can be accessed to adjust
 * the HSV threshold. It maintains six sliders, or seekbars, which represent
 * the minimum and maximum HSV.
 *
 * @author Calvin Yan
 */
public class SetThresholdActivity extends AppCompatActivity {

    private HSVSeekBar[] seekBars = new HSVSeekBar[6];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_threshold);

        for (int i = 0; i < 6; i++) {
            seekBars[i] = new HSVSeekBar(Constants.kSliderIds[i], Constants.kSliderReadoutIds[i],
                                         Constants.kSliderDefaultValues[i], Constants.kSliderNames[i], this);
        }
    }
}