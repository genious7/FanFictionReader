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
import android.widget.ToggleButton;

/**
 * Represents the browse menu where the category one is interested in is selected.
 * @author Michael Chen
 */
public class BrowseMenu extends Activity {
	
	private Context context;
	protected static final String COMMUNITIES = "Community";
	
	private final OnItemClickListener browseStoryListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id) {

				ToggleButton crossover = (ToggleButton)findViewById(R.id.xoverSelector);
				Intent i = new Intent(getApplicationContext(), CategoryMenu.class);
				i.putExtra("Id", (int)id);
				i.putExtra("Crossover", crossover.isChecked());
				startActivity(i);
			
		}
	};
	
	private final OnItemClickListener browseCommunitiesListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id) {
			if (id == 0) {
				Intent i = new Intent(context,CommunityMenu.class);
				i.putExtra(CommunityMenu.URL, "/communities/general/");
				startActivity(i);
			}else{
				Intent i = new Intent(getApplicationContext(),
						CategoryMenu.class);
				i.putExtra("Id", (int) id - 1);
				i.putExtra(CategoryMenu.COMMUNITY, true);
				startActivity(i);
			}		
		}
	};
	
	@Override 
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_list_view);
		context = this;	
		
		ListView category_menu= (ListView)findViewById(R.id.menuListView);
		
		String[] categories;
		
		if (getIntent().getBooleanExtra(COMMUNITIES, false)) {
			int length = getResources().getStringArray(R.array.category_button).length;
			categories = new String[length + 1];
			categories[0] = getString(R.string.communities_button);
			System.arraycopy(getResources().getStringArray(R.array.category_button),
					0, categories, 1, length);
			category_menu.setOnItemClickListener(browseCommunitiesListener);
		}else{
			View header = (View)getLayoutInflater().inflate(R.layout.browse_menu_header, null);
			category_menu.addHeaderView(header);
			categories = getResources().getStringArray(R.array.category_button);
			category_menu.setOnItemClickListener(browseStoryListener);
		}
		ArrayAdapter<String> browseAdapter= new ArrayAdapter<String>(this, R.layout.browse_menu_list_item, categories);
		category_menu.setAdapter(browseAdapter);	
	}
	
}
