package com.spicymango.fanfictionreader.menu.communitymenu;

import java.util.List;

import org.apache.commons.lang3.text.WordUtils;

import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.Settings;
import com.spicymango.fanfictionreader.menu.BaseFragment;
import com.spicymango.fanfictionreader.menu.communitymenu.CommunityMenuLoaders.*;
import com.spicymango.fanfictionreader.menu.storymenu.StoryMenuActivity;
import com.spicymango.fanfictionreader.util.Sites;

import android.content.Intent;
import android.content.UriMatcher;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Bundle;
import androidx.fragment.app.FragmentTransaction;
import androidx.loader.content.Loader;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.BaseAdapter;

public class CommunityMenuActivity extends AppCompatActivity {

	public static final class CommunityMenuFragment extends BaseFragment<CommunityMenuItem>{
		
		/**
		 * An interface that represents sortable members.
		 * @author Michael Chen
		 *
		 */
		interface Sortable{
			void sort(SortBy sortKey);
		}
		
		private static final int ARCHIVE_OF_OUR_OWN_COLLECTION = 2;
		private static final int FANFICTION_COMMUNITY = 0;
		private static final int FICTIONPRESS_COMMUNITY = 1;
		
		private static final UriMatcher URI_MATCHER;

		static{
			URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
			
			// FanFiction Communities
			URI_MATCHER.addURI(Sites.FANFICTION.AUTHORITY, "communities/general/", FANFICTION_COMMUNITY);
			URI_MATCHER.addURI(Sites.FANFICTION.AUTHORITY, "communities/*/*/", FANFICTION_COMMUNITY);
			URI_MATCHER.addURI(Sites.FANFICTION.AUTHORITY_DESKTOP, "communities/general/", FANFICTION_COMMUNITY);
			URI_MATCHER.addURI(Sites.FANFICTION.AUTHORITY_DESKTOP, "communities/*/*/", FANFICTION_COMMUNITY);
			
			// FictionPress Communities
			URI_MATCHER.addURI(Sites.FICTIONPRESS.AUTHORITY, "communities/general/", FICTIONPRESS_COMMUNITY);
			URI_MATCHER.addURI(Sites.FICTIONPRESS.AUTHORITY, "communities/*/*/", FICTIONPRESS_COMMUNITY);
			URI_MATCHER.addURI(Sites.FICTIONPRESS.AUTHORITY_DESKTOP, "communities/general/", FICTIONPRESS_COMMUNITY);
			URI_MATCHER.addURI(Sites.FICTIONPRESS.AUTHORITY_DESKTOP, "communities/*/*/", FICTIONPRESS_COMMUNITY);
			
			// Archive of Our Own Collections
			URI_MATCHER.addURI(Sites.ARCHIVE_OF_OUR_OWN.AUTHORITY, "collections", ARCHIVE_OF_OUR_OWN_COLLECTION);
		}
		
		private LoaderAdapter<CommunityMenuItem> mLoaderAdapter;

		@Override
		protected BaseAdapter adapter(List<CommunityMenuItem> dataSet) {
			return new CommunityAdapter(getActivity(), dataSet);
		}

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			setHasOptionsMenu(true);

			// Match the link, without any filters
			final Uri uri = removeFilters(getActivity().getIntent().getData());
			final int site = URI_MATCHER.match(uri);

			switch (site) {
			case FANFICTION_COMMUNITY:
				mLoaderAdapter = args -> new FanFictionCommunityLoader(getActivity(), args, uri);
				setTitle(R.string.menu_button_communities);
				setSubTitle(WordUtils.capitalize(uri.getLastPathSegment()));
				break;
			case FICTIONPRESS_COMMUNITY:
				mLoaderAdapter = args -> new FanFictionCommunityLoader(getActivity(), args, uri);
				setTitle(R.string.menu_button_communities);
				setSubTitle(WordUtils.capitalize(uri.getLastPathSegment()));
				break;
			case ARCHIVE_OF_OUR_OWN_COLLECTION:

				break;
			default:
				throw new IllegalStateException("CommunityMenuActivity: Invalid Uri" + uri);
			}
			
			mListView.setOnItemClickListener((parent, view, position, id) -> {
				final Intent i = new Intent(getActivity(), StoryMenuActivity.class);
				i.setData(getItem(position).uri);
				startActivity(i);
			});

			getLoaderManager().initLoader(0, mLoaderArgs, this);
		}

		@Override
		public Loader<List<CommunityMenuItem>> onCreateLoader(int id, Bundle args) {
			return mLoaderAdapter.getNewLoader(args);
		}
		
		@Override
		public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
			if (mLoader instanceof Sortable) {
				inflater.inflate(R.menu.community_menu, menu);				
			}
	    }

		@Override
		public boolean onOptionsItemSelected(MenuItem item){
			if (mLoader instanceof Sortable) {
				// Item can be sorted, pass the request to the loader				
				switch (item.getItemId()) {
				case R.id.community_sort_random:
					((Sortable) mLoader).sort(SortBy.RANDOM);
					return true;
				case R.id.community_sort_staff:
					((Sortable) mLoader).sort(SortBy.STAFF);
					return true;
				case R.id.community_sort_stories:
					((Sortable) mLoader).sort(SortBy.STORIES);
					return true;
				case R.id.community_sort_follows:
					((Sortable) mLoader).sort(SortBy.FOLLOWS);
					return true;
				case R.id.community_sort_date:
					((Sortable) mLoader).sort(SortBy.CREATE_DATE);
					return true;
				}
			}
			return false;
		}
	
		/**
		 * Removes any path elements that begin with a digit
		 * 
		 * @param uri
		 *            The {@link Uri} that should be formatted
		 * @return The formatted Uri.
		 */
		private Uri removeFilters(Uri uri) {
			// Remove any prior filters by only copying the segment of the path
			// that does not start with a number.
			Builder builder = uri.buildUpon();
			builder.path(null);
			List<String> pathSegments = uri.getPathSegments();
			for (String pathSegment : pathSegments) {
				if (!Character.isDigit(pathSegment.charAt(0))) {
					builder.appendPath(pathSegment);
				} else {
					break;
				}
			}
			builder.appendPath("");
			return builder.build();
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Settings.setOrientationAndThemeNoActionBar(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_frame_layout);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		if (savedInstanceState == null) {
			FragmentTransaction fr = getSupportFragmentManager().beginTransaction();
			fr.replace(R.id.content_frame, new CommunityMenuFragment());
			fr.commit();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
}
