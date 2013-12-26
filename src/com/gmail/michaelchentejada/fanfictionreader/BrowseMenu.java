/**
 * 
 */
package com.gmail.michaelchentejada.fanfictionreader;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ToggleButton;

/**
 * Represents the browse menu where the category one is interested in is selected.
 * @author Michael Chen
 */
public class BrowseMenu extends Activity {
	
	private Context context;
	
	@Override 
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_list_view);
		
		String[] categories = {	getResources().getString(R.string.category_button_anime),
								getResources().getString(R.string.category_button_books),
								getResources().getString(R.string.category_button_cartoons),
								getResources().getString(R.string.category_button_comics),
								getResources().getString(R.string.category_button_games),
								getResources().getString(R.string.category_button_misc),
								getResources().getString(R.string.category_button_movies),
								getResources().getString(R.string.category_button_plays),
								getResources().getString(R.string.category_button_tv)};
		
		ArrayAdapter<String> browseAdapter= new ArrayAdapter<String>(this, R.layout.browse_menu_list_item, categories);
		ListView category_menu= (ListView)findViewById(R.id.menuListView);
		
		View header = (View)getLayoutInflater().inflate(R.layout.browse_menu_header, null);
		category_menu.addHeaderView(header);
		
		category_menu.setAdapter(browseAdapter);
		
		context = this;
		
		category_menu.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id) {

					ToggleButton crossover = (ToggleButton)findViewById(R.id.xoverSelector);
					Intent i = new Intent(getApplicationContext(), CategoryMenu.class);
					i.putExtra("Id", (int)id);
					i.putExtra("Crossover", crossover.isChecked());
					startActivityForResult(i, 1);
				
			}
		
		});
		
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1 && resultCode == RESULT_CANCELED){
			int duration = Toast.LENGTH_SHORT;

			Toast toast = Toast.makeText(context, data.getStringExtra("Error"), duration);
			toast.show();
		}
		super.onActivityResult(requestCode, resultCode, data);
		
	}

}
