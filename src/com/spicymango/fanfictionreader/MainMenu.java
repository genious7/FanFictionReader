package com.spicymango.fanfictionreader;

import com.spicymango.fanfictionreader.R.attr;
import com.spicymango.fanfictionreader.activity.AccountActivity;
import com.spicymango.fanfictionreader.activity.LibraryMenuActivity;
import com.spicymango.fanfictionreader.activity.SearchAuthorActivity;
import com.spicymango.fanfictionreader.activity.SearchStoryActivity;
import com.spicymango.fanfictionreader.activity.StoryDisplayActivity;
import com.spicymango.fanfictionreader.activity.StoryMenuActivity;
import com.spicymango.fanfictionreader.dialogs.AboutDialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.content.res.Resources.Theme;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class MainMenu extends ActionBarActivity implements OnItemClickListener, OnClickListener {
	public static final String EXTRA_PREF = "Resume pref";
	public static final String EXTRA_RESUME_ID = "Resume Id";
	public static final String EXTRA_RESUME_CHAPTER = "Resume Chapter";
	public static final String EXTRA_RESUME_OFFSET = "Resume Offset";
	
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
		switch ((int)id) {
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
			DialogFragment diag = new AboutDialog();
			diag.show(getSupportFragmentManager(), null);
			break;
		case 7:
			SharedPreferences preference = getSharedPreferences(EXTRA_PREF,MODE_PRIVATE);
			long resumeId = preference.getLong(EXTRA_RESUME_ID, -1);
			if (resumeId == -1) {
				Toast toast = Toast.makeText(this, R.string.menu_toast_resume, Toast.LENGTH_SHORT);
				toast.show();
			}else{
				int resumeChap = preference.getInt(EXTRA_RESUME_CHAPTER, 1);
				int resumeOff = preference.getInt(EXTRA_RESUME_OFFSET, 0);
				i = new Intent (this, StoryDisplayActivity.class);
				i.setData(Uri.parse("file://fanfiction/" + resumeId + "_" + resumeChap + ".txt"));
				i.putExtra(StoryDisplayActivity.EXTRA_OFFSET, resumeOff);
				startActivity(i);
			}
			break;
		case 8:
			i = new Intent(this, AccountActivity.class);
			startActivity(i);
		default:
			break;
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case 0:
			Intent intent = getIntent();
			finish();
			startActivity(intent);
			break;
		default:
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Settings.setOrientationAndTheme(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_list_view);
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		final MenuItem menuItems[] = new MenuItem[] {
				new MenuItem(attr.ic_storage, 
						R.string.menu_button_my_library, 0),
				new MenuItem(attr.ic_action_replay,
						R.string.menu_button_resume, 7),
				new MenuItem(attr.ic_action_important,
						R.string.menu_button_favs_folls, 8),
				new MenuItem(attr.ic_folder_open,
						R.string.menu_button_browse_stories, 1),
				new MenuItem(attr.ic_action_view_as_list,
						R.string.menu_button_just_in, 2),
				new MenuItem(attr.ic_action_search,
						R.string.menu_button_search, 3),
				new MenuItem(attr.ic_action_group,
						R.string.menu_button_communities, 4),
				new MenuItem(attr.ic_action_settings,
						R.string.menu_button_settings, 5),
				new MenuItem(attr.ic_action_about,
						R.string.menu_button_about,	6),
				};

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
		}
		
		@Override
		public long getItemId(int position) {
			return getItem(position).id;
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
	       
	        MenuItem menuRow = getItem(position);
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
	private class MenuItem {
		public int icon;
		public String title;
		public final int id;
		
		/**
		 * Initializes a new menu item.
		 * @param Icon The Id of the image to be used as the icon
		 * @param Title The text to be used as the label
		 * @author Michael Chen
		 */
		public MenuItem(int Icon, int Title, int id){
			
			TypedValue typedValue = new TypedValue(); 
			Theme theme = MainMenu.this.getTheme();
			theme.resolveAttribute(Icon, typedValue, false);
			
			icon = typedValue.data;
			title = getString(Title);
			this.id = id;
		}
	}
}
