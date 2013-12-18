package com.gmail.michaelchentejada.fanfictionreader;

import android.R.drawable;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class MainMenu extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		setContentView(R.layout.activity_main_menu);
		
		MenuItem menuItems[] = new MenuItem[]{
				new MenuItem(drawable.ic_menu_agenda, getResources().getString(R.string.menu_button_my_library)),
				new MenuItem(drawable.ic_menu_manage, getResources().getString(R.string.menu_button_browse_stories)),
				new MenuItem(drawable.ic_menu_manage, getResources().getString(R.string.menu_button_just_in)),
				new MenuItem(drawable.ic_menu_search, getResources().getString(R.string.menu_button_search)),
				new MenuItem(drawable.ic_menu_manage, getResources().getString(R.string.menu_button_communities)),
				new MenuItem(drawable.ic_menu_preferences, getResources().getString(R.string.menu_button_settings)),
				new MenuItem(drawable.ic_menu_info_details, getResources().getString(R.string.menu_button_about))
		};
		
		MainMenuAdapter Adapter = new MainMenuAdapter(this, R.layout.main_menu_list_item, menuItems);
		ListView listView = (ListView) findViewById(R.id.mainMenuListView);
		listView.setAdapter(Adapter);
		
		listView.setOnItemClickListener(new OnItemClickListener() {
			
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int id,	long arg3) {
				Intent i;
				switch (id) {
				case 1:
					i = new Intent(getApplicationContext(), BrowseMenu.class);
					startActivity(i);
					break;
				default:
					break;
				}
				
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main_menu, menu);
		return true;
	}
}

