package com.spicymango.fanfictionreader.util.adapters;

import java.util.List;

import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.util.Story;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class StoryReducedAdapter extends ArrayAdapter<Story>{
	/**
	 * Creates a new StoryMenuAdapter
	 * @param context The current context
	 * @param objects The list of stories
	 */
	public StoryReducedAdapter(Context context, List<Story> objects) {
		super(context, R.layout.story_menu_list_item, objects);
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
			
			ViewHolder holder;
		
			if (convertView == null) {
				LayoutInflater inflater = ((Activity)getContext()).getLayoutInflater();
				convertView = inflater.inflate(R.layout.story_menu_list_item, parent, false);
				
				holder = new ViewHolder();
				holder.title = (TextView) convertView.findViewById(R.id.story_menu_list_item_title);
				holder.summary = (TextView) convertView.findViewById(R.id.story_menu_list_item_summary);
				holder.author = (TextView) convertView.findViewById(R.id.story_menu_list_item_author);
				convertView.findViewById(R.id.story_menu_list_item_words).setVisibility(View.GONE);
				convertView.findViewById(R.id.story_menu_list_item_follows).setVisibility(View.GONE);
				convertView.findViewById(R.id.story_menu_list_item_chapters).setVisibility(View.GONE);
				convertView.setTag(holder);				
			} else{
				holder = (ViewHolder) convertView.getTag();
			}
		    
			holder.title.setText(getItem(position).getName());
			holder.summary.setText(getItem(position).getSummary());
			holder.author.setText(getItem(position).getAuthor());
			holder.author.setTag(getItem(position).getAuthor_id());
		    return convertView;
	}
	
	/**
	 * A small helper class to hold the id's of the views
	 * @author Michael Chen
	 */
	private static final class ViewHolder{
		private TextView author;
		private TextView summary;
		private TextView title;
	}
}
