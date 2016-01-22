package com.spicymango.fanfictionreader.menu.reviewmenu;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.spicymango.fanfictionreader.R;

import java.text.DateFormat;
import java.util.List;

/**
 * Created by Michael Chen on 01/18/2016.
 */
public class ReviewAdapter extends ArrayAdapter<ReviewMenuItem> {
	private final DateFormat mDateFormatter;
	private final String mChapterFormat;

	public ReviewAdapter(Context context, List<ReviewMenuItem> objects) {
		super(context, R.layout.review_menu_list_item, objects);
		mDateFormatter = DateFormat.getDateInstance();
		mChapterFormat = context.getString(R.string.menu_reviews_chapter_posted);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;

		if (convertView == null){
			LayoutInflater inflater = ((Activity) getContext()).getLayoutInflater();
			convertView = inflater.inflate(R.layout.review_menu_list_item,parent, false);
			holder = new ViewHolder();
			holder.author = (TextView) convertView.findViewById(R.id.review_author);
			holder.text = (TextView) convertView.findViewById(R.id.review_text);
			holder.chapter = (TextView) convertView.findViewById(R.id.review_chapter);
			holder.date = (TextView) convertView.findViewById(R.id.review_date);
			convertView.setTag(holder);

			// Making the view clickable means that it will consume clicks instead of passing them
			// to the ListView. In doing so, the items are effectively disabled and won't be highlighted
			// when pressed. Note that this solution was used over overriding ArrayAdapter#isEnabled
			// because setting isEnabled to false removes the divider.
			convertView.setClickable(true);
		} else{
			holder = (ViewHolder) convertView.getTag();
		}

		ReviewMenuItem item = getItem(position);
		holder.author.setText(item.getAuthor());
		holder.text.setText(item.getText());
		holder.chapter.setText(String.format(mChapterFormat, item.getChapter()));
		holder.date.setText(mDateFormatter.format(item.getDate()));
		return convertView;
	}

	/**
	 * A small helper class to hold the id's of the views
	 *
	 * @author Michael Chen
	 */
	private static final class ViewHolder {
		private TextView author;
		private TextView text;
		private TextView chapter;
		private TextView date;
	}
}
