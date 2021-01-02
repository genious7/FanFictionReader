package com.spicymango.fanfictionreader.menu.librarymenu;

import android.content.Context;
import android.database.Cursor;
import androidx.cursoradapter.widget.SimpleCursorAdapter;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.provider.SqlConstants;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * A ViewBinder that adds the prefixes to the stories' details and binds each value to its respective
 * view.
 * @author Michael Chen
 *
 */
class LibraryBinder implements SimpleCursorAdapter.ViewBinder{
	/**
	 * Contains the templates for the stories' additional details
	 */
	private final String mWordFormat, mChapterFormat;
	private final DateFormat mDateFormat;
	private int mMaxChapterColumnIndex;

	/**
	 * Creates a new Library Binder.
	 *
	 * @param context The current context
	 */
	public LibraryBinder(Context context) {
		mWordFormat = context.getString(R.string.menu_library_words);
		mChapterFormat = context.getString(R.string.menu_library_chapters);
		mDateFormat = SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT);

		// Flag the max chapter index column as unknown
		mMaxChapterColumnIndex = -1;
	}

	@Override
	public boolean setViewValue(View v, Cursor c, int column) {
		// Create a variable to store text temporarily
		final String tmp;

		switch (v.getId()) {
		case R.id.story_menu_list_item_words:
			tmp = String.format(mWordFormat, c.getInt(column));
			((TextView)v).setText(tmp);
			return true;
		case R.id.story_menu_list_item_chapters:
			tmp = String.format(mChapterFormat, c.getInt(column));
			((TextView)v).setText(tmp);
			return true;
		case R.id.story_menu_list_item_follows:
			tmp = mDateFormat.format(c.getLong(column));
			((TextView)v).setText(tmp);
			return true;
		case R.id.completitionBar:

			// If the index of the story's chapter length is unknown, determine it
			if (mMaxChapterColumnIndex == -1){
				mMaxChapterColumnIndex = c.getColumnIndexOrThrow(SqlConstants.KEY_CHAPTER);
			}

			final ProgressBar bar = (ProgressBar) v;
			final int max = c.getInt(mMaxChapterColumnIndex);
			bar.setMax(max - 1);
			bar.setProgress(c.getInt(column) - 1);
			return true;
		default:
			return false;
		}
	}

}
