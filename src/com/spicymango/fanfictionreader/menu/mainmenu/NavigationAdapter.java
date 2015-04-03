package com.spicymango.fanfictionreader.menu.mainmenu;

import com.spicymango.fanfictionreader.R;

import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class NavigationAdapter extends BaseAdapter{
	private final DrawerItem mList[];
	private final ActionBarActivity mActivity;
	
	//TODO: Make nicer
	
	public NavigationAdapter(ActionBarActivity context, DrawerItem[] list) {
		mList = list;
		mActivity = context;
	}

	@Override
	public int getCount() {
		return mList.length;
	}

	@Override
	public Integer getItem(int position) {
		return mList[position].title;
	}

	@Override
	public long getItemId(int position) {
		return mList[position].id;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if (convertView == null) {
			convertView = mActivity.getLayoutInflater().inflate(R.layout.browse_menu_list_item, parent, false);
			TextView text = (TextView) convertView.findViewById(R.id.browse_menu_label);
			holder = new ViewHolder(text);
			convertView.setTag(holder);
		}else{
			holder = (ViewHolder) convertView.getTag();
		}
		
		holder.text.setText(mList[position].title);
		
		
		return convertView;
	}
	
	private static class ViewHolder{
		private final TextView text;
		
		public ViewHolder(TextView text) {
			this.text = text;
		}
	}
	
	public static final class DrawerItem{
		public final long id;
		public final int title;
		
		public DrawerItem(long id, int title) {
			this.id = id;
			this.title = title;
		}
	}
}