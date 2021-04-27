package com.spicymango.fanfictionreader.dialogs;

import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.Settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.fragment.app.DialogFragment;
import android.util.TypedValue;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class FontDialog extends DialogFragment implements OnSeekBarChangeListener, OnItemSelectedListener, OnClickListener {
	private static final int MIN_SIZE = 14;
	private static final int MAX_SIZE = 32;
	
	private TextView sampleText;
	private int fontSize;
	private Typeface fontFamily;
	private int fontFamilyId;
	
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		fontSize = Settings.fontSize(getActivity());
		
		View content = getActivity().getLayoutInflater().inflate(R.layout.dialog_font_picker,null);
		sampleText = (TextView) content.findViewById(R.id.diag_font_demo);
		sampleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
		
		SeekBar bar = (SeekBar) content.findViewById(R.id.diag_font_seek);
		bar.setMax(MAX_SIZE - MIN_SIZE);
		bar.setProgress(fontSize - MIN_SIZE);
		bar.setOnSeekBarChangeListener(this);
		
		Spinner spin = (Spinner)content.findViewById(R.id.diag_font_spinner);
		spin.setOnItemSelectedListener(this);
		spin.setSelection(Settings.getTypeFaceId(getActivity()));
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.pref_text_size);
		builder.setView(content);
		builder.setPositiveButton(android.R.string.ok, this);
		builder.setNegativeButton(android.R.string.cancel, null);
		
		return builder.create();
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		fontSize =  MIN_SIZE + progress;
		sampleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position,
			long id) {
		switch (position) {
		case 0:
			fontFamily = Typeface.SANS_SERIF;
			fontFamilyId = Settings.SANS_SERIF;
			break;
		case 1:
			fontFamily = Typeface.SERIF;
			fontFamilyId = Settings.SERIF;
			break;
		default:
			throw new UnsupportedOperationException();
		}
		sampleText.setTypeface(fontFamily);
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		switch (which) {
		case DialogInterface.BUTTON_POSITIVE:
			SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
			Editor e = pref.edit();
			e.putInt(getString(R.string.pref_key_text_size), fontSize);
			
			switch (fontFamilyId) {
			case Settings.SANS_SERIF:
				e.putInt(getString(R.string.pref_key_type_face), Settings.SANS_SERIF);
				break;
			case Settings.SERIF:
				e.putInt(getString(R.string.pref_key_type_face), Settings.SERIF);
				break;
			default:
				e.putInt(getString(R.string.pref_key_type_face), -1);
			}
			e.commit();
			break;
		default:
			break;
		}
	}
}
