package com.gmail.michaelchentejada.fanfictionreader;

import java.util.ArrayList;
import java.util.HashMap;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

public class FilterMenu extends Activity {
	
	private Spinner[] filterSpinner;
	private ArrayList<HashMap<String, Integer>> filterList = new ArrayList<HashMap<String, Integer>>();
	
	private OnClickListener runFilter = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			int[] result = new int[12];
			
			for (int i = 0; i < filterSpinner.length; i++) {
				result[i] = filterList.get(i).get(filterSpinner[i].getSelectedItem().toString());
			}
			
			Intent results = new Intent();
			results.putExtra("Filter", result);
			setResult(RESULT_OK, results);	
			finish();
		}
	};
	
	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_filter_view);
		setResult(RESULT_CANCELED);
		
		//Set Button Listener
		Button runButton = (Button)findViewById(R.id.filter_run);
		runButton.setOnClickListener(runFilter);
		
		//Load the filter list
		filterList = (ArrayList<HashMap<String, Integer>>) getIntent().getSerializableExtra("Filter List");
		ArrayList<ArrayList<String>> keys = (ArrayList<ArrayList<String>>) getIntent().getSerializableExtra("Keyset");
		
		//Error Detection
		if (filterList == null || keys == null) {
			Log.e("FilterMenu - onCreate", "Either filterList or keys is equal to null.");
			finish();
		}
		
		//Load Spinners
		filterSpinner = new Spinner[]{
				(Spinner)findViewById(R.id.filter_sort_option),
				(Spinner)findViewById(R.id.filter_time_range),
				(Spinner)findViewById(R.id.filter_genre_1),
				(Spinner)findViewById(R.id.filter_genre_2),
				(Spinner)findViewById(R.id.filter_rating),
				(Spinner)findViewById(R.id.filter_languague),
				(Spinner)findViewById(R.id.filter_length),
				(Spinner)findViewById(R.id.filter_status),
				(Spinner)findViewById(R.id.filter_character_a),
				(Spinner)findViewById(R.id.filter_character_b),
				(Spinner)findViewById(R.id.filter_character_c),
				(Spinner)findViewById(R.id.filter_character_d)
		};
		
		//Load values on spinners
		for (int i = 0; i < filterSpinner.length; i++) {
			ArrayAdapter<String> filterAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, keys.get(i));
			filterSpinner[i].setAdapter(filterAdapter);
		}
		
		//Set positions upon orientation change
		if (savedInstanceState != null) {
			int[] buffer = savedInstanceState.getIntArray("Spinner");
			for (int i = 0; i < filterSpinner.length; i++) {
				filterSpinner[i].setSelection(buffer[i]);
			}
		}
	}
	
	/**
	 * Saves the current position of the spinners
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		int[] buffer = new int[filterSpinner.length];
		for (int i = 0; i < filterSpinner.length; i++) {
			buffer[i] = filterSpinner[i].getSelectedItemPosition();
		}
		outState.putIntArray("Spinner", buffer);
		super.onSaveInstanceState(outState);
	}
}
