package com.spicymango.fanfictionreader.menu.authormenu;

import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;

import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.activity.Site;
import com.spicymango.fanfictionreader.activity.reader.StoryDisplayActivity;
import com.spicymango.fanfictionreader.dialogs.DetailDialog;
import com.spicymango.fanfictionreader.menu.BaseFragment;
import com.spicymango.fanfictionreader.menu.BaseLoader;
import com.spicymango.fanfictionreader.menu.storymenu.FilterDialog.FilterDialog;
import com.spicymango.fanfictionreader.util.Result;
import com.spicymango.fanfictionreader.util.Story;
import com.spicymango.fanfictionreader.util.adapters.StoryMenuAdapter;

import java.util.List;

/**
 * Displays the stories written by an author.
 *
 * Created by Michael Chen on 01/30/2016.
 */
public class AuthorStoryFragment extends BaseFragment<Story> implements FilterDialog.FilterListener{

	interface SubTitleGetter {
		String getSubTitle();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// The options menu for this fragment is the filter button
		setHasOptionsMenu(true);

		// Open the details menu when a story is long-pressed
		mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position,
										   long id) {
				DetailDialog.show(getActivity(), (Story) parent.getItemAtPosition(position));
				return true;
			}
		});
		mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				StoryDisplayActivity.openStory(getActivity(), id, Site.FANFICTION, true);
			}
		});

		// Initiate the loaders
		getLoaderManager().initLoader(0, savedInstanceState, this);
	}

	@Override
	public void onLoadFinished(Loader<List<Story>> loader, List<Story> data) {
		super.onLoadFinished(loader, data);

		if (mLoader.getState() == Result.SUCCESS && mLoader instanceof SubTitleGetter){
			setSubTitle(((SubTitleGetter)mLoader).getSubTitle());
		}

		if (mLoader instanceof BaseLoader.Filterable){
			getActivity().supportInvalidateOptionsMenu();
		}

	}

	@Override
	public Loader<List<Story>> onCreateLoader(int id, Bundle args) {
		return new AuthorStoryLoader.FanFictionAuthorStoryLoader(getActivity(), args, getActivity().getIntent().getData());
	}

	@Override
	public void onFilter(int[] selected) {
		BaseLoader.Filterable filterable = (BaseLoader.Filterable) mLoader;
		filterable.filter(selected);
	}

	@Override
	protected BaseAdapter adapter(List<Story> dataSet) {
		return new StoryMenuAdapter(getActivity(), dataSet);
	}

	@Override
	public void onResume() {
		super.onResume();
		getActivity().supportInvalidateOptionsMenu();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.story_menu, menu);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		MenuItem filter = menu.findItem(R.id.filter);
		if (mLoader instanceof BaseLoader.Filterable) {
			// If the menu can be filtered, enable the icon if ready
			filter.setVisible(true);
			if (((BaseLoader.Filterable) mLoader).isFilterAvailable()) {
				filter.setEnabled(true);
				filter.getIcon().setAlpha(255);
			} else {
				filter.setEnabled(false);
				filter.getIcon().setAlpha(64);
			}
		} else {
			// If the menu cannot be filtered, hide the icon.
			filter.setVisible(false);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.filter:
				BaseLoader.Filterable filterable = (BaseLoader.Filterable) mLoader;
				filterable.onFilterClick(getActivity());
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
}