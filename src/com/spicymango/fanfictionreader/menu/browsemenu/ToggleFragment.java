package com.spicymango.fanfictionreader.menu.browsemenu;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ToggleButton;

import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.activity.NavigationMenuActivity;

/**
 * The fragment that displays items that contain a toggle button in the action bar
 * @author Michael Chen
 */
public class ToggleFragment extends ListFragment implements LoaderCallbacks<List<BrowseMenuItem>>{
	public static final int STORIES_FANFICTION = 0;
	public static final int STORIES_FICTIONPRESS = 1;
	public static final int STORIES_ARCHIVE_OF_OUR_OWN = 2;
	public static final int COMMUNITIES_FANFICTION = 3;
	
	private List<BrowseMenuItem> mData;
	private ToggleLoader mLoader;
	private ToggleButton mToggleBtn;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		final int loaderId = getArguments().getInt(NavigationActivity.EXTRA_SITE);
		mLoader = (ToggleLoader) getLoaderManager().initLoader(loaderId, null, this);
		
		//Set the options menu after the loader has been initialized
		setHasOptionsMenu(true);
		
	}
	
	@Override
	public Loader<List<BrowseMenuItem>> onCreateLoader(int id, Bundle args) {
		switch (id){
		case STORIES_FANFICTION:
		case STORIES_FICTIONPRESS:
		case STORIES_ARCHIVE_OF_OUR_OWN:
		case COMMUNITIES_FANFICTION:
		default:
			throw new RuntimeException("ToggleFragment - Invalid id:" + id);
		}
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.browse_menu, menu);
		
		//Sets the visibility of the toggle button as required
		if (mLoader.hasToggle() == false) {
			MenuItem toggleBtn = menu.findItem(R.id.toggle_switch);
			toggleBtn.setVisible(false);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.activity_read_story, container, false);
		
		//Set toolbar
		Toolbar toolbar = (Toolbar) v.findViewById(R.id.toolbar);
		((ActionBarActivity)getActivity()).setSupportActionBar(toolbar);
		
		mToggleBtn = (ToggleButton) toolbar.findViewById(R.id.xoverSelector);
		
		return v;
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Intent i;
		i = new Intent(getActivity(), NavigationMenuActivity.class);
		
		if (mToggleBtn.isChecked()) {
			i.setData(mData.get(position).getUriCrossover());
		} else{
			i.setData(mData.get(position).getUriNormal());
		}
		
		startActivity(i);
	}
	
	@Override
	public void onLoaderReset(Loader<List<BrowseMenuItem>> loader) {
		mData = null;
	}
	
	@Override
	public void onLoadFinished(Loader<List<BrowseMenuItem>> loader,
			List<BrowseMenuItem> data) {
		mData = data;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			getActivity().onBackPressed();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onStart() {
		getActivity().setTitle(mLoader.getTitle());
	}
	
	public abstract class ToggleLoader extends Loader<List<BrowseMenuItem>>{

		public ToggleLoader(Context context) {
			super(context);
		}
		
		public abstract int getTitle();
		
		public abstract boolean hasToggle();
	}
}
