package com.spicymango.fanfictionreader.menu.storymenu.FilterDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.Settings;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

/**
 * A custom filter dialog. The dialog can implement spinners, checkboxes, and
 * labels. To instantiate the dialog, use the {@link Builder}. The calling
 * activity must implement the {@link FilterListener} interface.
 * 
 * @author Michael Chen
 */
public class FilterDialog extends DialogFragment implements OnClickListener {
	private final static int ROW_CHECKBOX = 0;
	private final static int ROW_DOUBLE_SPINNER = 2;
	private final static int ROW_HEADER = 1;
	private final static int ROW_SPINNER = 3;
	private final static int ROW_BLANK = 4;

	private final static String EXTRA_KEY = "Key";
	private final static String EXTRA_LABEL = "Label";
	private final static String EXTRA_ITEMS = "Items";

	private final static String STATE_SELECTED = "STATE_SELECTED";
	private final static String STATE_INITIAL = "STATE_INITIAL";
	private final static int SPINNER_RESOURCE = android.R.layout.simple_spinner_item;

	private List<View> mViews;
	private int[] mInitialPos;

	/**
	 * A simple interface that must be implemented by any activity that uses a
	 * {@link FilterDialog}. The method {@link #onFilter(int[])} will only be
	 * called when the user presses accept on the dialog if an option has been
	 * changed.
	 * 
	 * @author Michael Chen
	 *
	 */
	public interface FilterListener {
		/**
		 * Called whenever the filter dialog succeeds. Returns the currently
		 * selected positions of the spinners and checkboxes, in the same order
		 * in which they were added to the dialog. Any invalid spinners or
		 * checkboxes will have a selected position equal to zero.
		 * 
		 * @param selected
		 *            The selected spinner positions
		 */
		void onFilter(int[] selected);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		getDialog().setTitle(R.string.menu_story_filter_by);
		Context context = inflater.getContext();
		
		mViews = new ArrayList<>();

		// Load the variables stored by the builder
		Bundle arguments = getArguments();
		ArrayList<Integer> key = arguments.getIntegerArrayList(EXTRA_KEY);

		// Fetch the empty table layout from the xml file
		View v = inflater.inflate(R.layout.dialog_filter, container, false);
		final TableLayout tableLayout = (TableLayout) v.findViewById(R.id.filter_table);

		// Set the onClickListener for the apply button.
		View execute = v.findViewById(R.id.filter_run);
		execute.setOnClickListener(this);
		
		// Set the LayoutParams used by each row of the table
		final LayoutParams wrapBoth = new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		
		// Add the required spinners, checkboxes, and headers
		for (int i = 0; i < key.size(); i++) {
			// Create a new row in the table
			TableRow row = new TableRow(context);
			row.setLayoutParams(wrapBoth);
			row.setGravity(Gravity.CENTER_VERTICAL);

			switch (key.get(i)) {
			case ROW_CHECKBOX: {

				break;
			}
			case ROW_DOUBLE_SPINNER: {
				TextView label = new TextView(context);
				label.setText(arguments.getString(EXTRA_LABEL + i));
				label.setGravity(Gravity.CENTER_VERTICAL);

				TableRow.LayoutParams rowParam = new TableRow.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1);

				SpinnerData dataA = arguments.getParcelable(EXTRA_ITEMS + i + 'A');
				ArrayAdapter<String> filterAdapterA = new ArrayAdapter<>(context, SPINNER_RESOURCE, dataA.getLabels());
				Spinner spinnerA = new Spinner(context);
				spinnerA.setAdapter(filterAdapterA);
				spinnerA.setLayoutParams(rowParam);
				spinnerA.setSelection(dataA.getSelected());

				SpinnerData dataB = arguments.getParcelable(EXTRA_ITEMS + i + 'B');
				ArrayAdapter<String> filterAdapterB = new ArrayAdapter<>(context, SPINNER_RESOURCE, dataB.getLabels());
				Spinner spinnerB = new Spinner(context);
				spinnerB.setAdapter(filterAdapterB);
				spinnerB.setLayoutParams(rowParam);
				spinnerB.setSelection(dataB.getSelected());

				row.addView(label);
				row.addView(spinnerA);
				row.addView(spinnerB);
				mViews.add(spinnerA);
				mViews.add(spinnerB);
				break;
			}
			case ROW_HEADER: {
				TextView label = new TextView(context);
				label.setText(arguments.getString(EXTRA_LABEL + i));
				row.addView(label);
				break;
			}
			case ROW_SPINNER: {
				TextView label = new TextView(context);
				label.setText(arguments.getString(EXTRA_LABEL + i));
				label.setGravity(Gravity.CENTER_VERTICAL);

				TableRow.LayoutParams rowParam = new TableRow.LayoutParams(LayoutParams.MATCH_PARENT,
						LayoutParams.WRAP_CONTENT);
				rowParam.span = 2;

				SpinnerData data = arguments.getParcelable(EXTRA_ITEMS + i);
				ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(context, SPINNER_RESOURCE, data.getLabels());
				Spinner spinner = new Spinner(context);
				spinner.setAdapter(filterAdapter);
				spinner.setLayoutParams(rowParam);
				spinner.setSelection(data.getSelected());

				row.addView(label);
				row.addView(spinner);
				mViews.add(spinner);
				break;
			}
			case ROW_BLANK:{
				mViews.add(null);
				break;
			}
			}
			tableLayout.addView(row);
		}
		
		if (savedInstanceState == null) {
			// If the dialog is being opened and not recreated, store the
			// initial positions of the spinner.
			mInitialPos = getSelected();
		} else {
			// Restore any positions that were changed before the orientation
			// change.
			int savedPositions[] = savedInstanceState.getIntArray(STATE_SELECTED);
			for (int i = 0; i < savedPositions.length; i++) {
				final View view = mViews.get(i);
				
				if (view instanceof Spinner) {
					((Spinner) view).setSelection(savedPositions[i]);
				} else if (view instanceof CheckBox) {
					((CheckBox) view).setChecked(savedPositions[i] != 0);
				}
			}

			// Restore the initial positions
			mInitialPos = savedInstanceState.getIntArray(STATE_INITIAL);
		}
		return v;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		// Save the initial configuration
		outState.putIntArray(STATE_INITIAL, mInitialPos);
		
		// Save selected spinner item position
		int selected[] = getSelected();
		outState.putIntArray(STATE_SELECTED, selected);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.filter_run:
			int selected[] = getSelected();
			// Only call the onFilter method if the filters have changed.
			if (!Arrays.equals(selected, mInitialPos)) {
				FilterListener listener = (FilterListener) getActivity();
				listener.onFilter(selected);
			}			
			dismiss();
			break;
		default:
			break;
		}
	}

	/**
	 * Gets the currently selected options. Spinners will return the currently
	 * selected position while a checkbox will return 1 if it are checked and 0
	 * if it is not. or not.
	 * 
	 * @return The currently selected items
	 */
	private int[] getSelected() {
		int selected[] = new int[mViews.size()];
		
		// For each item, determine the type and get the appropriate value.
		for (int i = 0; i < selected.length; i++) {
			final View view = mViews.get(i);
			if (view instanceof Spinner) {
				selected[i] = ((Spinner) view).getSelectedItemPosition();
			} else if (view instanceof CheckBox) {
				selected[i] = ((CheckBox) view).isChecked() ? 1 : 0;
			}
		}
		return selected;
	}

	/**
	 * A builder used to create a new {@link FilterDialog}.
	 * 
	 * @author Michael Chen
	 *
	 */
	public final static class Builder {
		private Bundle mBundle;
		private ArrayList<Integer> mRowsAdded;
		private int rowId;

		/**
		 * Creates a builder for an empty filter dialog.
		 */
		public Builder() {
			mBundle = new Bundle();
			mRowsAdded = new ArrayList<>();
		}

		/**
		 * Adds a checkbox to the dialog
		 * 
		 * @param data
		 *            The {@link CheckboxData} that backs up the checkbox
		 */
		public void addCheckBox(CheckboxData data) {
			mRowsAdded.add(ROW_CHECKBOX);
			mBundle.putParcelable(EXTRA_ITEMS + rowId, data);
			rowId++;
		}

		/**
		 * Adds a simple label on an individual row.
		 * 
		 * @param header
		 *            The string to add as a header
		 */
		public void addHeader(String header) {
			rowId++;
			mRowsAdded.add(ROW_HEADER);
			mBundle.putString(EXTRA_LABEL + rowId, header);
		}
		
		/**
		 * Adds a spinner in a single row in the table. If data is null or if
		 * the list of spinner values is empty, the spinner is not displayed.
		 * 
		 * @param label
		 *            The spinner's label
		 * @param data
		 *            The {@link SpinnerData} backing up this spinner
		 */
		public void addSingleSpinner(String label, @Nullable SpinnerData data) {
			if (data == null || data.getLabels().isEmpty()) {
				mRowsAdded.add(ROW_BLANK);
				rowId++;
			} else {
				mRowsAdded.add(ROW_SPINNER);
				mBundle.putString(EXTRA_LABEL + rowId, label);
				mBundle.putParcelable(EXTRA_ITEMS + rowId, data);
				rowId++;
			}
		}

		/**
		 * Adds two spinners in a single row. If the data is null or if the list
		 * of spinner values is empty, the empty spinner is not displayed and
		 * the second spinner is shown in a single row.
		 * 
		 * @param label
		 *            The spinners' label
		 * @param spinner1
		 *            The {@link SpinnerData} for the first spinner
		 * @param spinner2
		 *            The SpinnerData for the second spinner
		 */
		public void addDoubleSpinner(String label, @Nullable SpinnerData spinner1, @Nullable SpinnerData spinner2) {
			if (spinner1 == null || spinner1.getLabels().isEmpty()) {
				if (spinner2 == null || spinner2.getLabels().isEmpty()) {
					// If there are no valid entries, insert two blank spots.
					mRowsAdded.add(ROW_BLANK);
					mRowsAdded.add(ROW_BLANK);
					rowId++;
					rowId++;
				} else {
					// If spinner1 is blank, just display spinner2
					mRowsAdded.add(ROW_BLANK);
					rowId++;
					addSingleSpinner(label, spinner2);
				}
			} else if (spinner2 == null || spinner2.getLabels().isEmpty()) {
				// If spinner2 is blank, just display the spinner1
				addSingleSpinner(label, spinner1);
				mRowsAdded.add(ROW_BLANK);
				rowId++;
			} else {
				// If the entry is valid, display both spinners
				mRowsAdded.add(ROW_DOUBLE_SPINNER);
				mBundle.putString(EXTRA_LABEL + rowId, label);
				mBundle.putParcelable(EXTRA_ITEMS + rowId + 'A', spinner1);
				mBundle.putParcelable(EXTRA_ITEMS + rowId + 'B', spinner2);
				rowId++;
			}
		}
		
		/**
		 * Builds and shows the FilterDialog. Note that the calling activity
		 * must implement the {@link FilterListener} interface.
		 * 
		 * @param activity The calling activity.
		 */
		public <T extends FragmentActivity & FilterListener>void show(T activity) {
			mBundle.putIntegerArrayList(EXTRA_KEY, mRowsAdded);

			FragmentManager manager = activity.getSupportFragmentManager();

			DialogFragment dialog = new FilterDialog();
			dialog.setArguments(mBundle);
			dialog.setStyle(STYLE_NORMAL, Settings.getDialogTheme(activity));
			dialog.show(manager, FilterDialog.class.getName());
		}
	}
}
