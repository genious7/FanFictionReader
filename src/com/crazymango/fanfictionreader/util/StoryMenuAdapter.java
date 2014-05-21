package com.crazymango.fanfictionreader.util;

import java.util.List;

import com.crazymango.fanfictionreader.R;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * An custom ArrayAdapter that display Story elements.
 * @author Michael Chen
 */
public class StoryMenuAdapter extends ArrayAdapter<Story> {
	
	/**
	 * Creates a new StoryMenuAdapter
	 * @param context The current context
	 * @param resource The id of the view representing a row
	 * @param objects The list of stories
	 */
	public StoryMenuAdapter(Context context, int resource, List<Story> objects) {
		super(context, resource, objects);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getItemId(int position) {
		return getItem(position).getId();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				LayoutInflater inflater = ((Activity)getContext()).getLayoutInflater();
				
				convertView = inflater.inflate(R.layout.story_menu_list_item, parent, false);
				
				ViewHolder viewHolder = new ViewHolder();
				viewHolder.title = (TextView) convertView.findViewById(R.id.story_menu_list_item_title);
				viewHolder.summary = (TextView) convertView.findViewById(R.id.story_menu_list_item_summary);
				viewHolder.author = (TextView) convertView.findViewById(R.id.story_menu_list_item_author);
				viewHolder.words = (TextView) convertView.findViewById(R.id.story_menu_list_item_words);
				viewHolder.follows = (TextView) convertView.findViewById(R.id.story_menu_list_item_follows);
				viewHolder.chapters = (TextView) convertView.findViewById(R.id.story_menu_list_item_chapters);
				convertView.setTag(viewHolder);				
			} 
			
			ViewHolder holder = (ViewHolder) convertView.getTag();
			
		    
			holder.title.setText(getItem(position).getName());
			holder.summary.setText(getItem(position).getSummary());
			holder.author.setText(getItem(position).getAuthor());
			holder.words.setText(String.valueOf(getItem(position).getWordLenght()));
			holder.follows.setText(String.valueOf(getItem(position).getFollows()));
			holder.chapters.setText(String.valueOf(getItem(position).getChapterLenght()));

		    return convertView;
	}
	
	/**
	 * A small helper class to hold the id's of the views
	 * @author Michael Chen
	 */
	private static final class ViewHolder{
		private TextView author;
		private TextView chapters;
		private TextView follows;
		private TextView summary;
		private TextView title;
		private TextView words;
	}
}