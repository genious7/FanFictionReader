package com.spicymango.fanfictionreader.util.adapters;

import java.util.List;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.TextView.BufferType;

import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.Settings;

/**
 * An adapter optimized for displaying long amounts of text efficiently
 * 
 * @author Michael Chen
 */
public class TextAdapter extends ArrayAdapter<Spanned> {
	private final int fontSize;
	private final Typeface tp;

	/**
	 * Creates a new instance of TextAdapter.
	 * 
	 * @param context The current context
	 * @param objects A list of paragraphs
	 */
	public TextAdapter(Context context, List<Spanned> objects) {
		super(context, 0, objects);
		fontSize = Settings.fontSize(context);
		tp = Settings.getTypeFace(context);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		TextView view;
		if (convertView == null) {
			LayoutInflater inflater = ((Activity) getContext()).getLayoutInflater();
			view = (TextView) inflater.inflate(R.layout.read_story_list_item, parent, false);
			view.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
			view.setTypeface(tp);
			view.setMovementMethod(MovementLinker.getInstance());
		} else {
			view = (TextView) convertView;
		}
		view.setText(getItem(position), BufferType.SPANNABLE);
		return view;
	}

	@Override
	public boolean isEnabled(int position) {
		return false;
	}

	/**
	 * A movement method that adds makes links clickable, silently catching any exceptions caused by
	 * erroneous links.
	 */
	private static class MovementLinker extends LinkMovementMethod {
		@Override
		public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
			try {
				return super.onTouchEvent(widget, buffer, event);
			} catch (ActivityNotFoundException ex) {
				// Swallow exceptions whenever they are caused by a link that has no corresponding activity
				return true;
			}
		}
	}
}