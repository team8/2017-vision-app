package com.frc8.team8vision.menu;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;

/**
 * Created by Alvin on 9/9/2017.
 */

public class StoredDoubleEntry implements TextWatcher {

	private double data_value = 0;

	private EditText textEntry;
	private int entryID;
	private String title;
	private String name;
	private SharedPreferences preferences;

	public StoredDoubleEntry(int textEntryID, String name, Activity activity){
		this.textEntry = (EditText)activity.findViewById(textEntryID);
		this.entryID = textEntryID;
		this.title = name;

		preferences = PreferenceManager.getDefaultSharedPreferences(activity);

		textEntry.addTextChangedListener(this);
	}

	public double getValue(){
		return data_value;
	}

	public void initProfiles(String profile, float default_value){
		this.name = this.title + "_" + profile;
		this.data_value = preferences.getFloat(name, default_value);
		textEntry.setText("" + this.data_value);
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {}
	@Override
	public void afterTextChanged(Editable s) {
		try {
			SharedPreferences.Editor editor = preferences.edit();
			String text_value = textEntry.getText().toString();
			data_value = Double.parseDouble(text_value);
			editor.putFloat(name, (float) data_value);
			editor.apply();
		} catch (NumberFormatException e){
			Log.d("FRC8.StoredDoubleEntry", "Invalid " + name + " value");
		}
	}
}
