/**
 * Michael Chen
 * April 22, 2014 
 */
package com.spicymango.fanfictionreader;

import com.spicymango.fanfictionreader.activity.CommunityMenuActivity;
import com.spicymango.fanfictionreader.activity.NavigationMenuActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
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
public class BrowseMenu extends ActionBarActivity implements OnItemClickListener{
	/**
	 * True if the current instance is a community, false otherwise
	 */
	private boolean isCommunity;
	
	private static final String[] FANFIC_URLS = {
	"anime",
	"book",
	"cartoon",
	"comic",
	"game",
	"misc",
	"movie",
	"play",
	"tv"};
	
	protected static final String COMMUNITIES = "Community";
	
	@Override 
	protected void onCreate(Bundle savedInstanceState){
		Settings.setOrientationAndTheme(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_list_view);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		ListView category_menu= (ListView)findViewById(R.id.list);
		isCommunity = getIntent().getBooleanExtra(COMMUNITIES, false);
		setTitle();
		
		String[] categories;
		
		if (isCommunity) {
			categories = getResources().getStringArray(R.array.category_button_community);
		}else{
			categories = getResources().getStringArray(R.array.category_button_normal);
		}
		category_menu.setOnItemClickListener(this);
		
		ArrayAdapter<String> browseAdapter= new ArrayAdapter<String>(this, R.layout.browse_menu_list_item, categories);
		category_menu.setAdapter(browseAdapter);	
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		Intent i;
		Uri.Builder builder = new Uri.Builder();
		builder.scheme(getString(R.string.fanfiction_scheme))
		.authority(getString(R.string.fanfiction_authority));
		
		if (isCommunity) {
			builder.appendPath("communities");
			if (id == 0) {
				i = new Intent(this, CommunityMenuActivity.class);
				builder.appendPath("general");
			} else {
				i = new Intent(this, NavigationMenuActivity.class);
				builder.appendPath(FANFIC_URLS[(int) (id - 1)]);
			}
		} else {
			ToggleButton crossover = (ToggleButton) findViewById(R.id.xoverSelector);
			i = new Intent(this, NavigationMenuActivity.class);
			if (crossover.isChecked()) {
				builder.appendPath("crossovers");
			}
			builder.appendPath(FANFIC_URLS[(int) id]);
		}
		builder.appendEncodedPath("");
		i.setData(builder.build());
		startActivity(i);

	}
	
	/**
	 * Sets the title
	 */
	private void setTitle(){
		int titleId = isCommunity ? R.string.menu_button_communities
				: R.string.menu_browse_title_stories;
		setTitle(titleId);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.browse_menu, menu);
		if (isCommunity) {
			MenuItem toggleBtn = menu.findItem(R.id.toggle_switch);
			toggleBtn.setVisible(false);
		}
		return true;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
