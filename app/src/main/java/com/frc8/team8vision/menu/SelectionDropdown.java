package com.frc8.team8vision.menu;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.frc8.team8vision.android.SettingsActivity;
import com.frc8.team8vision.util.Constants;
import com.frc8.team8vision.util.OnSelectionChangedCallback;
import com.frc8.team8vision.util.VisionUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Alvin on 9/11/2017.
 */

public class SelectionDropdown implements AdapterView.OnItemSelectedListener {

	private List<String> elements;

	private Spinner spinner;
	private int spinnerID;
	private String title;
	private String name;
	private boolean isDynamic;
	private OnSelectionChangedCallback callback;

	private SharedPreferences preferences;
	private Activity activity;

	public SelectionDropdown(int textEntryID, String name, List<String> elements, Activity activity,
							 boolean isDynamic, OnSelectionChangedCallback callback){
		this.spinnerID = textEntryID;
		this.title = name;
		this.isDynamic = isDynamic;
		this.callback = callback;
		this.activity = activity;
		this.spinner = (Spinner)activity.findViewById(textEntryID);

		preferences = PreferenceManager.getDefaultSharedPreferences(activity);

		if(isDynamic){
			Set<String> pref_elems = preferences.getStringSet(this.title+"_set", Collections.EMPTY_SET);
			if(!(pref_elems == null || pref_elems.isEmpty())){
				this.elements = new ArrayList<>(pref_elems);
			} else {
				this.elements = new ArrayList<>();
				for(String s: elements){
					this.elements.add(s);
				}
			}
		} else {
			this.elements = new ArrayList<>();
			for(String s: elements){
				this.elements.add(s.substring(0,1).toUpperCase() + s.substring(1).toLowerCase());
			}
		}

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity,
				android.R.layout.simple_spinner_item, this.elements);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);

		if(isDynamic){
			adapter.add("+ Add Item");
			Set<String> st = getSet();
			int idx = VisionUtil.getSetIndex(st, SettingsActivity.getProfile());
			spinner.setSelection(idx < 0 ? preferences.getInt(name+"_index", 0) : idx);
		}

		this.spinner.setOnItemSelectedListener(this);
	}

	public SelectionDropdown(int textEntryID, String name, Class<? extends Enum<?>> e, Activity activity,
							 boolean isDynamic, OnSelectionChangedCallback callback){
		this(textEntryID, name, new ArrayList<>(Arrays.asList(VisionUtil.enumToString(e))),
				activity, isDynamic, callback);
	}

	public void initProfiles(String profile){
		this.name = this.title + (this.isDynamic ? "" : "_" + profile);
		spinner.setSelection(preferences.getInt(this.name+"_index", 0));
	}

	private void addElement(String element){
		this.elements.add(0, element);
		SharedPreferences.Editor editor = preferences.edit();
		Set<String> st = new LinkedHashSet<String>(this.elements);
		st.remove("+ Add Item");
		editor.putStringSet(name+"_set", st);
		editor.apply();
	}

	private Set<String> getSet(){
		return preferences.getStringSet(name+"_set", null);
	}

	public String removeElement(String element){
		this.elements.remove(element);
		SharedPreferences.Editor editor = preferences.edit();
		Set<String> st = new LinkedHashSet<String>(this.elements);
		st.remove("+ Add Item");
		if(st.isEmpty()){
			this.elements.add(0, "Default");
			st.add("Default");
		}

		editor.putStringSet(name+"_set", st);
		editor.apply();

		String new_profile = this.elements.get(0);
		int idx = VisionUtil.getSetIndex(getSet(), new_profile);
		spinner.setSelection(idx < 0 ? 0 : idx);
		return new_profile;
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		String label = (String)parent.getItemAtPosition(position);

		final SharedPreferences.Editor editor = preferences.edit();
		editor.putInt(name+"_index", position);

		if(isDynamic && position == this.elements.size()-1){
			AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this.activity);
			final EditText et = new EditText(this.activity);
			alertDialogBuilder.setView(et);

			alertDialogBuilder.setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					String elem = et.getText().toString();
					addElement(elem);
					Set<String> st = getSet();
					int idx = VisionUtil.getSetIndex(st, elem);
					spinner.setSelection(0);
					editor.putInt(name+"_index", idx);
				}
			});

			AlertDialog alertDialog = alertDialogBuilder.create();
			alertDialog.show();
		} else {
			spinner.setSelection(position);
			if(this.callback != null){
				callback.selectionChanged(label);
			}
		}

		editor.apply();
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {

	}
}
