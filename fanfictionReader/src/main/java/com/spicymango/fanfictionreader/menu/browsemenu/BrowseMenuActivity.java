package com.spicymango.fanfictionreader.menu.browsemenu;

import android.content.Intent;
import android.content.UriMatcher;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentTransaction;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.core.view.MenuItemCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ToggleButton;

import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.Settings;
import com.spicymango.fanfictionreader.menu.BaseFragment;
import com.spicymango.fanfictionreader.menu.browsemenu.BrowseMenuLoaders.ArchiveOfOurOwnBrowseLoader;
import com.spicymango.fanfictionreader.menu.browsemenu.BrowseMenuLoaders.FanFictionCommunityBrowseLoader;
import com.spicymango.fanfictionreader.menu.browsemenu.BrowseMenuLoaders.FanFictionCrossOverBrowseLoader;
import com.spicymango.fanfictionreader.menu.browsemenu.BrowseMenuLoaders.FanFictionRegularBrowseLoader;
import com.spicymango.fanfictionreader.menu.browsemenu.BrowseMenuLoaders.FictionPressCommunityBrowseLoader;
import com.spicymango.fanfictionreader.menu.browsemenu.BrowseMenuLoaders.FictionPressPoetryPressBrowseLoader;
import com.spicymango.fanfictionreader.menu.browsemenu.BrowseMenuLoaders.FictionpressFictionPressBrowseLoader;
import com.spicymango.fanfictionreader.menu.categorymenu.CategoryMenuActivity;
import com.spicymango.fanfictionreader.menu.communitymenu.CommunityMenuActivity;
import com.spicymango.fanfictionreader.menu.storymenu.StoryMenuActivity;
import com.spicymango.fanfictionreader.util.Sites;

import java.util.List;
import java.util.Objects;

public class BrowseMenuActivity extends AppCompatActivity {

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home){
			onBackPressed();
			return true;
		} else{
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Settings.setOrientationAndThemeNoActionBar(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_frame_layout);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

		if (savedInstanceState == null) {
			FragmentTransaction fr = getSupportFragmentManager().beginTransaction();
			fr.replace(R.id.content_frame, new BrowseMenuFragment());
			fr.commit();
		}
	}

	public final static class BrowseMenuFragment extends BaseFragment<BrowseMenuItem>
			implements OnCheckedChangeListener {

		private final static UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

		private final static int SITE_ARCHIVE_OF_OUR_OWN = 0;
		private final static int SITE_FANFICTION = 1;
		private final static int SITE_FANFICTION_COMMUNITY = 2;
		private final static int SITE_FICTIONPRESS = 3;
		private final static int SITE_FICTIONPRESS_COMMUNITY = 4;

		private final static String STATE_TOGGLE_BUTTON = "STATE_TOGGLE";

		static {
			//Archive of Our Own
			URI_MATCHER.addURI(Sites.ARCHIVE_OF_OUR_OWN.AUTHORITY, null, SITE_ARCHIVE_OF_OUR_OWN);
			
			//FanFiction Mobile
			URI_MATCHER.addURI(Sites.FANFICTION.AUTHORITY, null, SITE_FANFICTION);
			URI_MATCHER.addURI(Sites.FANFICTION.AUTHORITY, "communities/", SITE_FANFICTION_COMMUNITY);
			//FanFiction Desktop
			URI_MATCHER.addURI(Sites.FANFICTION.AUTHORITY_DESKTOP, null, SITE_FANFICTION);
			URI_MATCHER.addURI(Sites.FANFICTION.AUTHORITY_DESKTOP, "communities/", SITE_FANFICTION_COMMUNITY);
			
			//FictionPress Mobile
			URI_MATCHER.addURI(Sites.FICTIONPRESS.AUTHORITY, null, SITE_FICTIONPRESS);
			URI_MATCHER.addURI(Sites.FICTIONPRESS.AUTHORITY, "communities/", SITE_FICTIONPRESS_COMMUNITY);
			//FictionPress Desktop
			URI_MATCHER.addURI(Sites.FICTIONPRESS.AUTHORITY_DESKTOP, null, SITE_FICTIONPRESS);
			URI_MATCHER.addURI(Sites.FICTIONPRESS.AUTHORITY_DESKTOP, "communities/", SITE_FICTIONPRESS_COMMUNITY);
		}

		private int mActiveLoaderId;
		private LoaderAdapter<BrowseMenuItem> mLoaderOff, mLoaderOn;
		private ToggleButton mToggle;

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);

			Uri uri = requireActivity().getIntent().getData();
			int siteId = URI_MATCHER.match(uri);

			switch (siteId) {
			case SITE_ARCHIVE_OF_OUR_OWN:
				setTitle(R.string.menu_browse_title_stories);
				setSubTitle(Sites.ARCHIVE_OF_OUR_OWN.TITLE);
				mLoaderOff = args -> new ArchiveOfOurOwnBrowseLoader(getActivity(), args);
				mListView.setOnItemClickListener((parent, view, position, id) -> {
					Intent i = new Intent(getActivity(), CategoryMenuActivity.class);
					i.setData(getItem(position).uri);
					startActivity(i);
				});
				break;
			case SITE_FANFICTION:
				setTitle(R.string.menu_browse_title_stories);
				setSubTitle(Sites.FANFICTION.TITLE);
				mLoaderOff = args -> new FanFictionRegularBrowseLoader(getActivity(), args);
				mLoaderOn = args -> new FanFictionCrossOverBrowseLoader(getActivity(), args);
				mListView.setOnItemClickListener((parent, view, position, id) -> {
					Intent i = new Intent(getActivity(), CategoryMenuActivity.class);
					i.setData(getItem(position).uri);
					startActivity(i);
				});
				enableToggleButton(R.string.toggle_regular, R.string.toggle_crossover, savedInstanceState);
				break;
			case SITE_FANFICTION_COMMUNITY:
				setTitle(R.string.menu_button_communities);
				setSubTitle(Sites.FANFICTION.TITLE);
				mLoaderOff = args -> new FanFictionCommunityBrowseLoader(getActivity(), args);
				mListView.setOnItemClickListener((parent, view, position, id) -> {
					Intent i;
					if (position == 0) {
						i = new Intent(getActivity(), CommunityMenuActivity.class);
					} else {
						i = new Intent(getActivity(), CategoryMenuActivity.class);
					}
					i.setData(getItem(position).uri);
					startActivity(i);
				});
				break;
			case SITE_FICTIONPRESS:
				setTitle(R.string.menu_browse_title_stories);
				setSubTitle(Sites.FICTIONPRESS.TITLE);
				mLoaderOff = args -> new FictionpressFictionPressBrowseLoader(getActivity(), args);
				mLoaderOn = args -> new FictionPressPoetryPressBrowseLoader(getActivity(), args);
				mListView.setOnItemClickListener((parent, view, position, id) -> {
					Intent i = new Intent(getActivity(), StoryMenuActivity.class);
					i.setData(getItem(position).uri);
					startActivity(i);
				});
				enableToggleButton(R.string.toggle_fiction, R.string.toggle_poetry, savedInstanceState);
				break;
			case SITE_FICTIONPRESS_COMMUNITY:
				setTitle(R.string.menu_button_communities);
				setSubTitle(Sites.FICTIONPRESS.TITLE);
				mLoaderOff = args -> new FictionPressCommunityBrowseLoader(getActivity(), args);
				mListView.setOnItemClickListener((parent, view, position, id) -> {
					Intent i;
					if (position == 0) {
						i = new Intent(getActivity(), CommunityMenuActivity.class);
					} else {
						i = new Intent(getActivity(), CategoryMenuActivity.class);
					}
					i.setData(getItem(position).uri);
					startActivity(i);
				});
				break;
			default:
				throw new IllegalArgumentException();
			}

			if (mToggle == null) {
				mActiveLoaderId = 0;
				LoaderManager.getInstance(this).initLoader(0, mLoaderArgs, this);
			} else {
				mActiveLoaderId = mToggle.isChecked() ? 1 : 0;
				LoaderManager.getInstance(this).initLoader(mActiveLoaderId, mLoaderArgs, this);
			}
		}

		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			mActiveLoaderId = isChecked ? 1 : 0;
			LoaderManager.getInstance(this).initLoader(mActiveLoaderId, null, this);
		}

		@NonNull
		@Override
		public Loader<List<BrowseMenuItem>> onCreateLoader(int id, Bundle args) {
			switch (id) {
			case 0:
				return mLoaderOff.getNewLoader(args);
			case 1:
			default:
				return mLoaderOn.getNewLoader(args);
			}
		}

		@Override
		public void onCreateOptionsMenu(Menu menu, @NonNull MenuInflater inflater) {
			MenuItem item = menu.add("");
			item.setActionView(mToggle);
			item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			super.onCreateOptionsMenu(menu, inflater);
		}

		@Override
		public void onLoadFinished(@NonNull Loader<List<BrowseMenuItem>> loader, List<BrowseMenuItem> data) {
			if (loader.getId() == mActiveLoaderId) {
				super.onLoadFinished(loader, data);
			}
		}

		@Override
		public void onSaveInstanceState(@NonNull Bundle outState) {
			super.onSaveInstanceState(outState);
			if (mToggle != null) {
				outState.putBoolean(STATE_TOGGLE_BUTTON, mToggle.isChecked());
			}
		}

		private void enableToggleButton(@StringRes int textOff, @StringRes int textOn, Bundle savedInstanceState) {
			mToggle = new ToggleButton(getActivity());
			mToggle.setTextOff(requireActivity().getString(textOff));
			mToggle.setTextOn(requireActivity().getString(textOn));
			mToggle.setOnCheckedChangeListener(this);
			if (savedInstanceState != null) mToggle.setChecked(savedInstanceState.getBoolean(STATE_TOGGLE_BUTTON));
			else mToggle.setChecked(false);
			setHasOptionsMenu(true);
		}

		@Override
		protected BaseAdapter adapter(List<BrowseMenuItem> dataSet) {
			return new BrowseMenuAdapter(getActivity(), dataSet);
		}
	}
}
