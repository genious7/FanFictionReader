package com.spicymango.fanfictionreader.activity;

import java.util.List;

import org.jsoup.nodes.Document;

import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.activity.reader.StoryDisplayActivity;
import com.spicymango.fanfictionreader.dialogs.DetailDialog;
import com.spicymango.fanfictionreader.menu.BaseActivity;
import com.spicymango.fanfictionreader.menu.BaseLoader.Filterable;
import com.spicymango.fanfictionreader.menu.storymenu.FilterDialog.FilterDialog;
import com.spicymango.fanfictionreader.menu.storymenu.FilterDialog.SpinnerData;
import com.spicymango.fanfictionreader.menu.storymenu.FilterDialog.FilterDialog.FilterListener;
import com.spicymango.fanfictionreader.util.Parser;
import com.spicymango.fanfictionreader.util.SearchLoader;
import com.spicymango.fanfictionreader.util.Sites;
import com.spicymango.fanfictionreader.util.Story;
import com.spicymango.fanfictionreader.util.adapters.StoryMenuAdapter;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.SearchView.OnQueryTextListener;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;

/**
 * An activity used to search for stories
 * @author Michael Chen
 */
public class SearchStoryActivity extends BaseActivity<Story> implements OnQueryTextListener, FilterListener{
	private SearchLoader<Story> mLoader;
	private SearchView sView;
	
	@NonNull
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

		sView = (SearchView) menu.findItem(R.id.search).getActionView();
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
	public void onLoadFinished(@NonNull Loader<List<Story>> loader, List<Story> data) {
		super.onLoadFinished(loader, data);
		mLoader = (SearchLoader<Story>) loader;
		supportInvalidateOptionsMenu();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.filter){
			((Filterable) mLoader).onFilterClick(this);
			return true;
		} else{
			return super.onOptionsItemSelected(item);

		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem filter = menu.findItem(R.id.filter);
		if (mLoader == null || !((Filterable) mLoader).isFilterAvailable()) {
			filter.setEnabled(false);
			filter.getIcon().setAlpha(64);
		} else {
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
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		LoaderManager.getInstance(this).initLoader(0, savedInstanceState, this);
	}
	
	private static class StorySearchLoader extends SearchLoader<Story> implements Filterable{

		public StorySearchLoader(Context context, Bundle savedInstanceState) {
			super(context, savedInstanceState);
		}

		@Override
		protected Uri getUri(int currentPage) {
			// Don't load anything on empty queries.
			if (TextUtils.isEmpty(mQuery)) return null;

			Uri.Builder builder = Sites.FANFICTION.BASE_URI.buildUpon();
			builder.path("search/").
				appendQueryParameter("type", "story")
				.appendQueryParameter("ready", "1")
				.appendQueryParameter("keywords", mQuery.trim().replace(' ', '+'))
				.appendQueryParameter("ppage", currentPage + "");

			// Adds the filter, if available
			if (isFilterAvailable()) {
				for (SpinnerData spinnerData : mFilterData) {
					final String key = spinnerData.getName();
					final String value = spinnerData.getCurrentFilter();
					if (key == null) continue;
					builder.appendQueryParameter(key, value);
				}
			}
			return builder.build();
		}
		
		@Override
		protected boolean load(Document document, List<Story> list) {
			// Load the filters if they aren't already loaded.
			if (!isFilterAvailable()) {
				mFilterData = SearchFilter(document);
			}
			return Parser.Stories(document, list);
		}

		@Override
		protected void resetFilter() {
			filter = new int[]{0,0,1,0,0,0,0,0,0,0,0,0,0,0};	
		}

		
		@Override
		public void onFilterClick(@NonNull FragmentActivity activity) {
			FilterDialog.Builder builder = new FilterDialog.Builder();	
			builder.addSingleSpinner(activity.getString(R.string.filter_type), mFilterData.get(0));
			builder.addSingleSpinner(activity.getString(R.string.filter_category), mFilterData.get(1));
			builder.addSingleSpinner(activity.getString(R.string.filter_sort), mFilterData.get(2));
			builder.addSingleSpinner(activity.getString(R.string.filter_date), mFilterData.get(3));
			builder.addDoubleSpinner(activity.getString(R.string.filter_genre), mFilterData.get(4), mFilterData.get(5));
			builder.addSingleSpinner(activity.getString(R.string.filter_rating), mFilterData.get(6));
			builder.addSingleSpinner(activity.getString(R.string.filter_language), mFilterData.get(7));
			builder.addSingleSpinner(activity.getString(R.string.filter_length), mFilterData.get(8));
			builder.addSingleSpinner(activity.getString(R.string.filter_status), mFilterData.get(9));
			builder.addDoubleSpinner(activity.getString(R.string.filter_character), mFilterData.get(10),
									 mFilterData.get(11));
			builder.addDoubleSpinner(activity.getString(R.string.filter_character), mFilterData.get(12),
									 mFilterData.get(13));
			builder.show((SearchStoryActivity) activity);
		}

		@Override
		public boolean isFilterAvailable() {
			return mFilterData != null;
		}

		@Override
		public void filter(int[] filterSelected) {
			for (int i = 0; i < mFilterData.size(); i++) {
				mFilterData.get(i).setSelected(filterSelected[i]);
			}
			filter = filterSelected;
			resetState();
			startLoading();
		}

	}

	@Override
	public void onFilter(int[] selected) {
		((Filterable) mLoader).filter(selected);
	}	
}