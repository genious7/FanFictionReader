package com.gmail.michaelchentejada.fanfictionreader.util;

import java.util.List;

import com.gmail.michaelchentejada.fanfictionreader.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class StoryMenuAdapter extends ArrayAdapter<Story> {
	private final Context context;
	private List<Story> list;
	
	public StoryMenuAdapter(Context context, int resource, List<Story> objects) {
		super(context, resource, objects);
		this.context = context;
		this.list = objects;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
			View rowView = convertView;
			if (rowView == null) {
				LayoutInflater inflater = (LayoutInflater) context
		        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				rowView = inflater.inflate(R.layout.story_menu_list_item, parent, false);
				
				ViewHolder viewHolder = new ViewHolder();
				viewHolder.title = (TextView) rowView.findViewById(R.id.story_menu_list_item_title);
				viewHolder.summary = (TextView) rowView.findViewById(R.id.story_menu_list_item_summary);
				viewHolder.author = (TextView) rowView.findViewById(R.id.story_menu_list_item_author);
				viewHolder.words = (TextView) rowView.findViewById(R.id.story_menu_list_item_words);
				viewHolder.follows = (TextView) rowView.findViewById(R.id.story_menu_list_item_follows);
				viewHolder.chapters = (TextView) rowView.findViewById(R.id.story_menu_list_item_chapters);
				rowView.setTag(viewHolder);				
			} 
			
			ViewHolder holder = (ViewHolder) rowView.getTag();
			
		    
			holder.title.setText(list.get(position).getName());
			holder.summary.setText(list.get(position).getSummary());
			holder.author.setText(list.get(position).getAuthor());
			holder.words.setText(String.valueOf(list.get(position).getWordLenght()));
			holder.follows.setText(String.valueOf(list.get(position).getFollows()));
			holder.chapters.setText(String.valueOf(list.get(position).getChapterLenght()));

		    return rowView;
	}
	
	static class ViewHolder{
		TextView title;
	    TextView summary;
	    TextView author;
	    TextView words;
	    TextView follows;
	    TextView chapters;
	}
	
}
