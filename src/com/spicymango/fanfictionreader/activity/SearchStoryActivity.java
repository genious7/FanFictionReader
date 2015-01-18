package com.spicymango.fanfictionreader.activity;

import java.util.List;

import org.jsoup.nodes.Document;

import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.activity.reader.StoryDisplayActivity;
import com.spicymango.fanfictionreader.dialogs.DetailDialog;
import com.spicymango.fanfictionreader.filter.FilterDialog;
import com.spicymango.fanfictionreader.util.BaseActivity;
import com.spicymango.fanfictionreader.util.Parser;
import com.spicymango.fanfictionreader.util.SearchLoader;
import com.spicymango.fanfictionreader.util.Story;
import com.spicymango.fanfictionreader.util.adapters.StoryMenuAdapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Toast;

/**
 * An activity used to search for stories
 * @author Michael Chen
 */
public class SearchStoryActivity extends BaseActivity<Story> implements OnQueryTextListener{
	private SearchLoader<Story> mLoader;
	private SearchView sView;
	
	@Override
	public Loader<List<Story>> onCreateLoader(int id, Bundle args) {
		mLoader = new StorySearchLoader(this, args);
		super.mLoader = mLoader;
		return mLoader;
	}
	
	/**
	 * Initializes the menu on the action bar.
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.search_menu, menu);

		sView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.search));
		sView.setOnQueryTextListener(this);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		StoryDisplayActivity.openStory(this, id, Site.FANFICTION, true);
	}
	
	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view,
			int position, long id) {
		DetailDialog.show(this, mList.get(position));
		return true;
	}
	
	@Override
	public void onLoadFinished(Loader<List<Story>> loader, List<Story> data) {
		super.onLoadFinished(loader, data);
		mLoader = (SearchLoader<Story>) loader;
		supportInvalidateOptionsMenu();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.filter:
			FilterDialog.show(this, mLoader.mFilterList, mLoader.filter);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem filter = menu.findItem(R.id.filter);
		if (mLoader == null || mLoader.mFilterList == null) {
			filter.setEnabled(false);
			filter.getIcon().setAlpha(64);
		}else{
			filter.setEnabled(true);
			filter.getIcon().setAlpha(255);
		}
		return super.onPrepareOptionsMenu(menu);
	}
	
	@Override
	public boolean onQueryTextChange(String arg0) {
		return false;
	}

	@Override
	public boolean onQueryTextSubmit(String arg0) {
		sView.clearFocus();
		mLoader.search(arg0);
		return true;
	}

	@Override
	protected BaseAdapter getAdapter() {
		return new StoryMenuAdapter(this, mList);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode==1){//Filter Menu
			if (resultCode==RESULT_CANCELED) {
				//Dialog cancelled
				Toast toast = Toast.makeText(this, getResources().getString(R.string.dialog_cancelled), Toast.LENGTH_SHORT);
				toast.show();
			}else if (resultCode == RESULT_OK) {
				int[] filter = data.getIntArrayExtra(FilterDialog.RESULT);
				mLoader.filter = filter;
				mLoader.resetState();
				mLoader.startLoading();
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportLoaderManager().initLoader(0, savedInstanceState, this);
	}
	
	private static class StorySearchLoader extends SearchLoader<Story>{

		public StorySearchLoader(Context context, Bundle savedInstanceState) {
			super(context, savedInstanceState);
		}

		@Override
		protected Uri formatUri(int currentPage) {
			Uri.Builder builder = BASE_URI.buildUpon();
			builder.path("search.php")
					.appendQueryParameter("type", "story")
					.appendQueryParameter("ready", "1")
					.appendQueryParameter("keywords", mQuery)
					.appendQueryParameter("categoryid", filter[13] + "")
					.appendQueryParameter("genreid", filter[2] + "")
					.appendQueryParameter("languageid", filter[5] + "")
					.appendQueryParameter("censorid", filter[4] + "")
					.appendQueryParameter("statusid", filter[7] + "")
					.appendQueryParameter("ppage", currentPage + "")
					.appendQueryParameter("words", filter[6] + "");
			return builder.build();
		}
		
		@Override
		protected boolean load(Document document, List<Story> list) {
			// Load the filters if they aren't already loaded.
			if (mFilterList == null) {
				mFilterList = SearchFilter(document);
			}
			return Parser.Stories(document, list);
		}

		@Override
		protected void resetFilter() {
			filter = new int[]{1,0,0,0,0,0,0,0,0,0,0,0,0,0};	
		}

	}	
}