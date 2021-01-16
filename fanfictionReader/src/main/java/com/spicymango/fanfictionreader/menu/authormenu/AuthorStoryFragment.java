package com.spicymango.fanfictionreader.menu.authormenu;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.BaseAdapter;
import android.widget.TextView;

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
 * Displays the stories written by an author or in the author's favorites. Requires as an argument
 * the type of loader that should be used, as either the author favorites or the author's own
 * stories may be displayed.
 * <p/>
 * Created by Michael Chen on 01/30/2016.
 */
public class AuthorStoryFragment extends BaseFragment<Story> implements FilterDialog.FilterListener{
	public static final String EXTRA_LOADER_ID = "Loader ID";
	public static final int LOADER_STORIES = 0;
	public static final int LOADER_FAVORITES = 1;

	interface SubTitleGetter {
		String getSubTitle();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// Display a textView if no stories are found
		final TextView empty = new TextView(getActivity());
		empty.setText(R.string.menu_author_no_stories);
		empty.setGravity(Gravity.CENTER);
		empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
		setEmptyView(empty);

		// The options menu for this fragment is the filter button
		setHasOptionsMenu(true);

		// Open the details menu when a story is long-pressed
		mListView.setOnItemLongClickListener((parent, view, position, id) -> {
			DetailDialog.show(getActivity(), (Story) parent.getItemAtPosition(position));
			return true;
		});
		mListView.setOnItemClickListener((parent, view, position, id) -> StoryDisplayActivity.openStory(getActivity(), id, Site.FANFICTION, true));


		// Try to get the loader id
		final Bundle arguments = getArguments();
		if (arguments == null || !arguments.containsKey(EXTRA_LOADER_ID))
			throw new IllegalArgumentException("The argument EXTRA_LOADER_ID is mandatory");
		final int loaderId = arguments.getInt(EXTRA_LOADER_ID);

		// Initiate the loaders
		LoaderManager.getInstance(this).initLoader(loaderId, mLoaderArgs, this);
	}

	@Override
	public void onLoadFinished(@NonNull Loader<List<Story>> loader, List<Story> data) {
		super.onLoadFinished(loader, data);

		if (mLoader.getState() == Result.SUCCESS && mLoader instanceof SubTitleGetter){
			setSubTitle(((SubTitleGetter)mLoader).getSubTitle());
		}

		if (mLoader instanceof BaseLoader.Filterable){
			requireActivity().invalidateOptionsMenu();
		}

	}

	@NonNull
	@Override
	public Loader<List<Story>> onCreateLoader(int id, Bundle args) {
		final BaseLoader<Story> loader;
		switch (id){
			case LOADER_STORIES:
				loader = new AuthorStoryLoader.
						FanFictionAuthorStoryLoader(getActivity(), args, requireActivity().getIntent().getData());
				break;
			case LOADER_FAVORITES:
				loader = new AuthorStoryLoader.
						FanFictionAuthorFavoriteLoader(getActivity(), args, requireActivity().getIntent().getData());
				break;
			default:
				throw new IllegalArgumentException("The EXTRA_LOADER_ID '" + id + "' is invalid");
		}
		return loader;
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
		requireActivity().invalidateOptionsMenu();
	}

	@Override
	public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
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
		if (item.getItemId() == R.id.filter){
			BaseLoader.Filterable filterable = (BaseLoader.Filterable) mLoader;
			filterable.onFilterClick(getActivity());
			return true;
		} else{
			return super.onOptionsItemSelected(item);
		}
	}
}