package com.gmail.michaelchentejada.fanfictionreader;

import com.gmail.michaelchentejada.fanfictionreader.activity.AboutActivity;
import com.gmail.michaelchentejada.fanfictionreader.activity.LibraryMenuActivity;
import com.gmail.michaelchentejada.fanfictionreader.activity.SearchActivity;
import com.gmail.michaelchentejada.fanfictionreader.activity.StoryMenuActivity;
import com.gmail.michaelchentejada.fanfictionreader.util.MainMenuAdapter;
import com.gmail.michaelchentejada.fanfictionreader.util.MenuItem;

import android.R.drawable;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class MainMenu extends Activity implements OnItemClickListener {

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
		case 3:
			i = new Intent(this, SearchActivity.class);
			startActivity(i);
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
}
