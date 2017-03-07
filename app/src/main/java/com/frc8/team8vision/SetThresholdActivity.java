package com.frc8.team8vision;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;

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

	public void onCheckboxClicked(View view){
		// Is the box now checked
		boolean checked = ((CheckBox) view).isChecked();

		// Check which box is checked
		switch (view.getId()){
			case R.id.flashlightEnabled:
				if(checked){
					MainActivity.toggleFlash();
				}
		}
	}
}