package com.crazymango.fanfictionreader;

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
	
	private static final String SPINNER_POSITION = "Spinners";
	
	//References to data coming from parent activity
	public static final String KEYSET = "Keyset";
	public static final String STATE_FILTER_LIST = "Filter List";
	public static final String SELECTED_KEYS = "Selected Keys";
	
	private OnClickListener runFilter = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			int[] result = new int[filterSpinner.length];
			
			//Obtain the currently selected position values
			for (int i = 0; i < filterSpinner.length; i++) {
				if (filterSpinner[i].getSelectedItem() == null){
					result[i] = 0;
				}else{
					result[i] = filterList.get(i).get(filterSpinner[i].getSelectedItem().toString());
				}
			}
			
			//Pass them back to the parent activity
			Intent results = new Intent();
			results.putExtra(STATE_FILTER_LIST, result);
			results.putExtra(SELECTED_KEYS, selectedPositions());
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
		filterList = (ArrayList<HashMap<String, Integer>>) getIntent().getSerializableExtra(STATE_FILTER_LIST);
		ArrayList<ArrayList<String>> keys = (ArrayList<ArrayList<String>>) getIntent().getSerializableExtra(KEYSET);
		
		//Error Detection
		if (filterList == null || keys == null) {
			Log.e("FilterMenu - onCreate", "Either filterList or keys is equal to null.");
			finish();
		}
		
		//Load Spinners
		filterSpinner = new Spinner[]{
				(Spinner)findViewById(R.id.filter_sort_options),
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
				(Spinner)findViewById(R.id.filter_character_d),
				(Spinner)findViewById(R.id.filter_type),
				(Spinner)findViewById(R.id.filter_category),
		};
		
		View[] tableRows = new View[]{
				findViewById(R.id.tableRow1),
				findViewById(R.id.tableRow2),
				findViewById(R.id.tableRow3),
				findViewById(R.id.tableRow3),
				findViewById(R.id.tableRow4),
				findViewById(R.id.tableRow5),
				findViewById(R.id.tableRow6),
				findViewById(R.id.tableRow7),
				findViewById(R.id.tableRow8),
				findViewById(R.id.tableRow8),
				findViewById(R.id.tableRow8),
				findViewById(R.id.tableRow8),
				findViewById(R.id.tableRow9),
				findViewById(R.id.tableRow10),
		};
		
		for (View view : tableRows) {
			view.setVisibility(View.GONE);
		}
		
		//Load values on spinners
		for (int i = 0; i < filterSpinner.length; i++) {
			ArrayAdapter<String> filterAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, keys.get(i));
			filterSpinner[i].setAdapter(filterAdapter);		
			if (!keys.get(i).isEmpty()) {			
				tableRows[i].setVisibility(View.VISIBLE);
			}else{
				filterSpinner[i].setVisibility(View.GONE);
			}
		}
		
		//Set positions upon orientation change
		if (savedInstanceState != null) {
			int[] buffer = savedInstanceState.getIntArray(SPINNER_POSITION);
			for (int i = 0; i < filterSpinner.length; i++) {
				filterSpinner[i].setSelection(buffer[i]);
			}
		}else{ //Set positions to what was present on previous search.
			int[] selected_keys = getIntent().getIntArrayExtra(SELECTED_KEYS);
			if (selected_keys != null) {
				for (int i = 0; i < filterSpinner.length; i++) {
					filterSpinner[i].setSelection(selected_keys[i]);
				}
			}
		}
	}
	
	/**
	 * Saves the current position of the spinners
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putIntArray(SPINNER_POSITION, selectedPositions());
		super.onSaveInstanceState(outState);
	}
	
	/**
	 * Returns the selected positions of every currently displayed spinner.
	 * @return The selected position of each spinner, in the order dictated by filterSpinner
	 */
	private int[] selectedPositions(){
		int[] buffer = new int[filterSpinner.length];
		for (int i = 0; i < filterSpinner.length; i++) {
			buffer[i] = filterSpinner[i].getSelectedItemPosition();
		}
		return buffer;
	}
}
