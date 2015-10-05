package com.spicymango.fanfictionreader.filter;

import java.util.ArrayList;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;

@Deprecated
public class FilterDialog {

	public final static int RESULT_CODE = 1;
	public final static String RESULT = FilterMenu.EXTRA_FILTER;

	/**
	 * Shows the filter dialog
	 * @param activity A handle to the current activity
	 * @param filter The list containing filter and key pairs
	 * @param keys The last selected keys, or null if none exist
	 */
	@Deprecated
	public static <T extends Map<String, Integer>> void show(Activity activity,
			ArrayList<T> filter, int[] keys) {
		
		Intent i = new Intent(activity, FilterMenu.class);
		i.putExtra(FilterMenu.EXTRA_FILTER, filter);

		// Generate the ordered KeySet, as putExtra does not support ordered
		// HashMaps
		ArrayList<ArrayList<String>> keyset = new ArrayList<ArrayList<String>>();
		for (int j = 0; j < filter.size(); j++) {
			keyset.add(new ArrayList<String>(filter.get(j).keySet()));
		}

		i.putExtra(FilterMenu.EXTRA_KEYSET, keyset);
		i.putExtra(FilterMenu.EXTRA_VALUES, keys);
		activity.startActivityForResult(i, RESULT_CODE);
	}
}
