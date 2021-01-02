package com.spicymango.fanfictionreader.menu.browsemenu;

import java.util.List;

import com.spicymango.fanfictionreader.R;

import android.content.Context;
import androidx.appcompat.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

final class BrowseMenuAdapter extends ArrayAdapter<BrowseMenuItem> {

	public BrowseMenuAdapter(Context context, List<BrowseMenuItem> mList) {
		super(context, R.layout.browse_menu_list_item, mList);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		BrowseMenuHolder holder = null;

		if (convertView == null) {
			LayoutInflater inflater = ((AppCompatActivity) getContext()).getLayoutInflater();
			convertView = inflater.inflate(R.layout.browse_menu_list_item, parent, false);

			holder = new BrowseMenuHolder();
			holder.txt = (TextView) convertView.findViewById(android.R.id.text1);
			convertView.setTag(holder);
		} else {
			holder = (BrowseMenuHolder) convertView.getTag();
		}

		holder.txt.setText(getItem(position).title);
		return convertView;
	}

	private static final class BrowseMenuHolder {
		private TextView txt;
	}

}