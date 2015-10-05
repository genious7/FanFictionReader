package com.spicymango.fanfictionreader.menu.communitymenu;

import java.util.List;

import com.spicymango.fanfictionreader.R;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * An adapter that binds the {@link CommunityMenuItem} to a {@link ListView}.
 * @author Michael Chen
 *
 */
final class CommunityAdapter extends ArrayAdapter<CommunityMenuItem> {

	/**
	 * Creates a new {@link CommunityAdapter}
	 * 
	 * @param context
	 *            The current {@link Context}
	 * @param objects
	 *            The {@link List} of CommunityMenuItems that should be
	 *            displayed.
	 */
	public CommunityAdapter(Context context, List<CommunityMenuItem> objects) {
		super(context, R.layout.community_menu_list_item, objects);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		CommunityAdapter.ViewHolder holder;
		if (convertView == null) {
			LayoutInflater inflater = ((Activity) getContext()).getLayoutInflater();
			convertView = inflater.inflate(R.layout.community_menu_list_item, parent, false);
			holder = new ViewHolder();
			holder.title = (TextView) convertView.findViewById(R.id.community_title);
			holder.summary = (TextView) convertView.findViewById(R.id.community_summary);
			holder.author = (TextView) convertView.findViewById(R.id.community_author);
			holder.stories = (TextView) convertView.findViewById(R.id.community_stories);
			convertView.setTag(holder);
		} else {
			holder = (CommunityAdapter.ViewHolder) convertView.getTag();
		}

		holder.title.setText(getItem(position).title);
		holder.summary.setText(getItem(position).summary);
		holder.author.setText(getItem(position).author);
		holder.stories.setText(String.valueOf(getItem(position).stories));

		return convertView;
	}

	/**
	 * A small helper class to hold the id's of the views
	 * 
	 * @author Michael Chen
	 */
	private static final class ViewHolder {
		private TextView author;
		private TextView title;
		private TextView stories;
		private TextView summary;
	}
}