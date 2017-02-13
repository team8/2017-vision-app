package com.frc8.team8vision;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class SetThresholdActivity extends AppCompatActivity {

    private HSVSeekBar[] seekBars = new HSVSeekBar[6];

    private int[] sliderIds = {R.id.hLow, R.id.sLow, R.id.vLow, R.id.hHigh, R.id.sHigh, R.id.vHigh},
                  displayIds = {R.id.hLowInfo, R.id.sLowInfo, R.id.vLowInfo,
                                R.id.hHighInfo, R.id.sHighInfo, R.id.vHighInfo},
                  defValues = {0, 0, 0, 180, 255, 255};

    private String[] titles = {"Minimum Hue", "Minimum Saturation", "Minimum Value",
                               "Maximum Hue", "Maximum Saturation", "Maximum Value"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_threshold);

        for (int i = 0; i < 6; i++) {
            seekBars[i] = new HSVSeekBar(sliderIds[i], displayIds[i], defValues[i], titles[i], this);
        }
    }
}