package com.spicymango.fanfictionreader;

import com.spicymango.fanfictionreader.activity.AboutActivity;
import com.spicymango.fanfictionreader.activity.LibraryMenuActivity;
import com.spicymango.fanfictionreader.activity.SearchAuthorActivity;
import com.spicymango.fanfictionreader.activity.SearchStoryActivity;
import com.spicymango.fanfictionreader.activity.StoryMenuActivity;

import android.R.drawable;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class MainMenu extends Activity implements OnItemClickListener, OnClickListener {

	@Override
	public void onClick(DialogInterface dialog, int which) {
		Intent i;
		switch (which) {
		case 0:
			i = new Intent(this, SearchStoryActivity.class);
			break;
		case 1:
			i = new Intent(this, SearchAuthorActivity.class);
			break;
		default:
			return;
		}
		startActivity(i);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		Intent i;
		switch (position) {
		case 0:
			i = new Intent(this, LibraryMenuActivity.class);
			startActivity(i);
			break;
		case 1:// Case Browse Stories
			i = new Intent(this, BrowseMenu.class);
			startActivity(i);
			break;
		case 2:// Case Just In
			i = new Intent(this, StoryMenuActivity.class);
			i.setData(Uri.parse("https://m.fanfiction.net/j/"));
			startActivity(i);
			break;
		case 3://Search
			
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			String[] label = getResources().getStringArray(R.array.menu_search_by);
			builder.setItems( label, this);
			builder.create();
			builder.show();
			break;
		case 4:// Communities
			i = new Intent(this, BrowseMenu.class);
			i.putExtra(BrowseMenu.COMMUNITIES, true);
			startActivity(i);
			break;
		case 5: // Case Settings
			i = new Intent(this, Settings.class);
			startActivityForResult(i, 0);
			break;
		case 6:
			i = new Intent(this, AboutActivity.class);
			startActivity(i);
		default:
			break;
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case 0:
			Settings.setOrientation(this);
			break;
		default:
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_list_view);
		Settings.setOrientation(this);
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		final MenuItem menuItems[] = new MenuItem[] {
				new MenuItem(drawable.ic_menu_agenda, getResources().getString(
						R.string.menu_button_my_library)),
				new MenuItem(R.drawable.ic_folder_open, getResources()
						.getString(R.string.menu_button_browse_stories)),
				new MenuItem(R.drawable.ic_action_view_as_list, getResources()
						.getString(R.string.menu_button_just_in)),
				new MenuItem(drawable.ic_menu_search, getResources().getString(
						R.string.menu_button_search)),
				new MenuItem(R.drawable.ic_action_group, getResources()
						.getString(R.string.menu_button_communities)),
				new MenuItem(R.drawable.ic_action_settings, getResources()
						.getString(R.string.menu_button_settings)),
				new MenuItem(drawable.ic_menu_info_details, getResources()
						.getString(R.string.menu_button_about)) };

		MainMenuAdapter Adapter = new MainMenuAdapter(this,
				R.layout.main_menu_list_item, menuItems);
		ListView listView = (ListView) findViewById(R.id.list);
		listView.setAdapter(Adapter);

		listView.setOnItemClickListener(this);
	}
	
	/**
	 * The custom menu adapter for the main menu. Contains only an image and a single text.
	 * @author Michael Chen
	 */
	private static class MainMenuAdapter extends ArrayAdapter<MenuItem> {
		private MenuItem data[]=null;
		private int layoutResourceId;
		
		/**
		 * Initializes the adapter
		 * @param context The current context
		 * @param layoutResourceId The resource ID for a layout file
		 * @param data The objects to represent in the list view
		 */
		public MainMenuAdapter(Context context, int layoutResourceId, MenuItem[] data) {
			super(context, layoutResourceId, data);
			this.layoutResourceId=layoutResourceId;
			this.data=data;
		}
		
		@Override 
		public View getView(int position, View convertView, ViewGroup parent){
			View row = convertView;
			MenuItemHolder holder = null;
			if(row == null)
	        {
	            LayoutInflater inflater = ((Activity)getContext()).getLayoutInflater();
	            row = inflater.inflate(layoutResourceId, parent, false);
	           
	            holder = new MenuItemHolder();
	            holder.imgIcon = (ImageView)row.findViewById(R.id.list_item_icon);
	            holder.txtTitle = (TextView)row.findViewById(R.id.list_item_title);
	           
	            row.setTag(holder);
	        }
	        else
	        {
	            holder = (MenuItemHolder)row.getTag();
	        }
	       
	        MenuItem menuRow = data[position];
	        holder.txtTitle.setText(menuRow.title);
	        holder.imgIcon.setImageResource(menuRow.icon);
	       
	        return row;
	    }
	   
		/**
		 * A cache of the ImageView and the TextView. Provides a speed improvement.
		 * @author Michael Chen
		 */
		private static class MenuItemHolder
	    {
	        private ImageView imgIcon;
	        private TextView txtTitle;
	    }
	}


	/**
	 * Represents a single menu item
	 * @author Michael Chen
	 */
	private static class MenuItem {
		public int icon;
		public String title;
		
		/**
		 * Initializes a new menu item.
		 * @param Icon The Id of the image to be used as the icon
		 * @param Title The text to be used as the label
		 * @author Michael Chen
		 */
		public MenuItem(int Icon, String Title){
			icon = Icon;
			title = Title;
		}
	}
}
