package com.spicymango.fanfictionreader.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;


import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.spicymango.fanfictionreader.R;

@Deprecated
public class FilterMenu extends AppCompatActivity {
	
	/**
	 * Used to save the position of the spinners upon orientation change
	 */
	private static final String STATE_SPINNER = "Spinners";
	
	/**
	 * Key to a list of hashMap containing the entry pairs
	 */
	protected static final String EXTRA_FILTER = "Filter List";
	
	/**
	 * Key to a list of the ordered entries
	 */
	protected static final String EXTRA_KEYSET = "Keyset";
	
	/**
	 * Key to the previously selected values, if any
	 */
	protected static final String EXTRA_VALUES = "Selected values";
	
	/**
	 * Gets the key corresponding to a certain value
	 * @param map The map to search in
	 * @param value The value whose key is desired
	 * @return The key
	 */
	public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
	    for (Entry<T, E> entry : map.entrySet()) {
	        if (value.equals(entry.getValue())) {
	            return entry.getKey();
	        }
	    }
	    return null;
	}
	
	private ArrayList<HashMap<String, Integer>> filterList = new ArrayList<HashMap<String, Integer>>();
	private Spinner[] filterSpinner;	
	private ArrayList<ArrayList<String>> keys;
	
	private final OnClickListener runFilter = new OnClickListener() {
		
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
			results.putExtra(EXTRA_FILTER, result);
			setResult(RESULT_OK, results);	
			finish();
		}
	};
	
	/**
	 * Obtains the positions of the spinners corresponding to the values
	 * supplied
	 * 
	 * @param filterValues
	 *            The values supplied
	 * @return The positions in the spinners
	 */
	private int[] positionsFromValues(int[] filterValues) {

		int[] positions = new int[filterList.size()];
		String key;

		for (int i = 0; i < filterList.size(); i++) {
			key = getKeyByValue(filterList.get(i), filterValues[i]);
			positions[i] = key == null ? 0 : keys.get(i).indexOf(key);
		}
		return positions;
	}

	/**
	 * Returns the selected positions of every currently displayed spinner.
	 * @return The selected position of each spinner, in the order dictated by filterSpinner
	 */
	private int[] selectedPositions() {
		int[] buffer = new int[filterSpinner.length];
		for (int i = 0; i < filterSpinner.length; i++) {
			buffer[i] = filterSpinner[i].getSelectedItemPosition();
		}
		return buffer;
	}
	
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
		filterList = (ArrayList<HashMap<String, Integer>>) getIntent().getSerializableExtra(EXTRA_FILTER);
		keys = (ArrayList<ArrayList<String>>) getIntent().getSerializableExtra(EXTRA_KEYSET);
		
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
				findViewById(R.id.tableRow7),
				findViewById(R.id.tableRow6),
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

		// Set positions upon orientation change
		if (savedInstanceState != null) {
			int[] buffer = savedInstanceState.getIntArray(STATE_SPINNER);
			for (int i = 0; i < filterSpinner.length; i++) {
				filterSpinner[i].setSelection(buffer[i]);
			}
		} else { // Set positions to what was present on previous search.

			int[] filterValues = getIntent().getIntArrayExtra(EXTRA_VALUES);
			if (filterValues != null) {
				filterValues = positionsFromValues(filterValues);
				for (int i = 0; i < filterSpinner.length; i++) {
					filterSpinner[i].setSelection(filterValues[i]);
				}
			}
		}
	}
	
	/**
	 * Saves the current position of the spinners
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putIntArray(STATE_SPINNER, selectedPositions());
		super.onSaveInstanceState(outState);
	}
}
