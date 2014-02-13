package com.gmail.michaelchentejada.fanfictionreader;

import com.gmail.michaelchentejada.fanfictionreader.util.MainMenuAdapter;
import com.gmail.michaelchentejada.fanfictionreader.util.MenuItem;
import com.gmail.michaelchentejada.fanfictionreader.util.currentState;

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

public class MainMenu extends Activity {

	private final OnItemClickListener Listener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int id,	long arg3) {
			Intent i;
			switch (id) {
			case 0:
				i = new Intent(getApplicationContext(), LibraryMenu.class);
				startActivity(i);
				break;
			case 1://Case Browse Stories
				i = new Intent(getApplicationContext(), BrowseMenu.class);
				startActivity(i);
				break;
			case 2://Case Just In
				i = new Intent(getApplicationContext(), StoryMenu.class);
				i.setData(Uri.parse("https://m.fanfiction.net/j/"));
				i.putExtra(Menu.EXTRA_ACTIVITY_STATE, currentState.JUSTIN);
				startActivity(i);
				break;
			case 3:
				i = new Intent(getApplicationContext(), SearchMenu.class);
				startActivity(i);
				break;
			case 4://Communities
				i = new Intent(getApplicationContext(), BrowseMenu.class);
				i.putExtra(BrowseMenu.COMMUNITIES, true);
				startActivity(i);
				break;
			case 5: //Case Settings
				i = new Intent(getApplicationContext(), Settings.class);
				startActivity(i);
				break;
			default:
				break;
			}
			
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		setContentView(R.layout.activity_list_view);
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		
		final MenuItem menuItems[] = new MenuItem[]{
				new MenuItem(drawable.ic_menu_agenda, getResources().getString(R.string.menu_button_my_library)),
				new MenuItem(drawable.ic_menu_manage, getResources().getString(R.string.menu_button_browse_stories)),
				new MenuItem(drawable.ic_menu_manage, getResources().getString(R.string.menu_button_just_in)),
				new MenuItem(drawable.ic_menu_search, getResources().getString(R.string.menu_button_search)),
				new MenuItem(R.drawable.ic_action_group, getResources().getString(R.string.menu_button_communities)),
				new MenuItem(R.drawable.ic_action_settings, getResources().getString(R.string.menu_button_settings)),
				new MenuItem(drawable.ic_menu_info_details, getResources().getString(R.string.menu_button_about))
		};
		
		MainMenuAdapter Adapter = new MainMenuAdapter(this, R.layout.main_menu_list_item, menuItems);
		ListView listView = (ListView) findViewById(R.id.menuListView);
		listView.setAdapter(Adapter);
		
		listView.setOnItemClickListener(Listener);
	}
}

