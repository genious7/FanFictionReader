package com.spicymango.fanfictionreader.menu.librarymenu;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.Loader;
import androidx.core.view.MenuItemCompat;
import androidx.cursoradapter.widget.SimpleCursorAdapter;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.Settings;
import com.spicymango.fanfictionreader.activity.Site;
import com.spicymango.fanfictionreader.activity.reader.StoryDisplayActivity;
import com.spicymango.fanfictionreader.dialogs.DetailDialog;
import com.spicymango.fanfictionreader.menu.authormenu.AuthorMenuActivity;
import com.spicymango.fanfictionreader.menu.reviewmenu.ReviewMenuActivity;
import com.spicymango.fanfictionreader.menu.storymenu.FilterDialog.FilterDialog.FilterListener;
import com.spicymango.fanfictionreader.provider.SqlConstants;
import com.spicymango.fanfictionreader.provider.StoryProvider;
import com.spicymango.fanfictionreader.services.LibraryDownloader;
import com.spicymango.fanfictionreader.util.FileHandler;
import com.spicymango.fanfictionreader.util.Sites;
import com.spicymango.fanfictionreader.util.Story;

/**
 * An activity which displays the stories that are saved in the library.
 *
 * @author Michael Chen
 */
public class LibraryMenuActivity extends AppCompatActivity implements FilterListener {
	private LibraryMenuFragment mFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Settings.setOrientationAndThemeNoActionBar(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_frame_layout);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		assert getSupportActionBar() != null; // Keeps IntelliJ null check happy
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		final String tag = LibraryMenuFragment.class.getName();
		if (savedInstanceState == null) {

			mFragment = new LibraryMenuFragment();
			FragmentTransaction fr = getSupportFragmentManager().beginTransaction();
			fr.replace(R.id.content_frame, mFragment, tag);
			fr.commit();
		} else {

			mFragment = (LibraryMenuFragment) getSupportFragmentManager().findFragmentByTag(tag);
			if (mFragment == null) {
				throw new RuntimeException("LibraryMenuActivity - onCreate: mFragment == null");
			}
		}
	}

	@Override
	public void onFilter(int[] selected) {
		mFragment.onFilter(selected);
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

	/**
	 * A fragment that displays the stories stored in the library.
	 *
	 * @author Michael Chen
	 */
	public static final class LibraryMenuFragment extends Fragment implements LoaderCallbacks<Cursor>, SqlConstants, OnItemClickListener, FilterListener {

		private static final int LOADER_LIBRARY = 0;

		private static final int[] DEST_PROJECTION = {
				R.id.story_menu_list_item_title,
				R.id.story_menu_list_item_summary,
				R.id.story_menu_list_item_author,
				R.id.story_menu_list_item_chapters,
				R.id.story_menu_list_item_words,
				R.id.story_menu_list_item_follows,
				R.id.completitionBar};

		private static final String[] GET_PROJECTION = {KEY_STORY_ID, KEY_TITLE,
														KEY_SUMMARY, KEY_AUTHOR, KEY_CHAPTER, KEY_LENGTH, KEY_UPDATED,
														KEY_LAST, KEY_CATEGORY, KEY_OFFSET};

		private static final String[] TO_PROJECTION = {KEY_TITLE, KEY_SUMMARY,
													   KEY_AUTHOR, KEY_CHAPTER, KEY_LENGTH, KEY_UPDATED, KEY_LAST};

		private SimpleCursorAdapter mAdapter;
		private View mProgressBar;

		private LibraryLoader mLoader;
		private FrameLayout mEmptyView;
		private ListView mListView;

		@Nullable
		@Override
		public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
								 @Nullable Bundle savedInstanceState) {
			final View v = inflater.inflate(R.layout.activity_list_view, container, false);

			// Attach the listeners to the ListView and add the footer
			mListView = (ListView) v.findViewById(android.R.id.list);
			mListView.setOnItemClickListener(this);
			final View footer = inflater.inflate(R.layout.footer_list, mListView, false);
			mListView.addFooterView(footer);

			// Add the adapter to the listView
			mAdapter = new SimpleCursorAdapter(getContext(), R.layout.library_menu_list_item, null, TO_PROJECTION, DEST_PROJECTION, 0);
			mAdapter.setViewBinder(new LibraryBinder(getContext()));
			mListView.setAdapter(mAdapter);
			registerForContextMenu(mListView);

			// Hide the nextPage and the retry buttons; these will not be used in this fragment
			v.findViewById(R.id.story_load_pages).setVisibility(View.GONE);
			v.findViewById(R.id.row_retry).setVisibility(View.GONE);

			// The progress bar is used while the CursorLoader loads
			mProgressBar = v.findViewById(R.id.progress_bar);

			// Set the empty view
			mEmptyView = (FrameLayout) v.findViewById(R.id.empty);
			final TextView empty = new TextView(getActivity());
			empty.setText(R.string.menu_library_no_stories);
			empty.setGravity(Gravity.CENTER);
			empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
			mEmptyView.addView(empty);

			setHasOptionsMenu(true);

			return v;
		}

		@Override
		public void onActivityCreated(@Nullable Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			getLoaderManager().initLoader(LOADER_LIBRARY, savedInstanceState, this);
		}

		@Override
		public void onCreateContextMenu(ContextMenu menu, View v,
										ContextMenuInfo menuInfo) {
			// Inflate the basic menu
			getActivity().getMenuInflater().inflate(R.menu.library_context_menu, menu);

			// Obtain the id that is being loaded
			final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
			final long storyId = info.id;

			// Use the id to obtain the Story corresponding Story object
			final Uri databaseUri = Uri.withAppendedPath(StoryProvider.FF_CONTENT_URI, String.valueOf(storyId));
			final Cursor c = getContext().getContentResolver().query(databaseUri, null, null, null, null);

			final Story story;
			if (c != null && c.moveToFirst()) {
				story = Story.fromCursor(c);
				c.close();
			} else {
				story = null;
			}

			// Gray out the "view reviews" option if required.
			final MenuItem reviewItem = menu.findItem(R.id.menu_library_context_view_reviews);
			reviewItem.setEnabled(story != null && story.getReviews() != 0);
		}

		@Override
		public boolean onContextItemSelected(MenuItem item) {
			final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
			long storyId = info.id;

			// Use the id to obtain the Story corresponding Story object
			final Uri databaseUri = Uri.withAppendedPath(StoryProvider.FF_CONTENT_URI, String.valueOf(storyId));
			final Cursor c = getContext().getContentResolver().query(databaseUri, null, null, null, null);

			final Story story;
			if (c != null && c.moveToFirst()) {
				story = Story.fromCursor(c);
				c.close();
			} else {
				Log.e("LibraryMenuActivity", "The cursor is null");
				return true;
			}

			switch (item.getItemId()) {
				case R.id.menu_library_context_details:
					DetailDialog.show(getActivity(), story);
					return true;

				case R.id.menu_library_context_delete:
					final AlertDialog.Builder diag = new AlertDialog.Builder(getContext());
					diag.setTitle(R.string.dialog_remove);
					diag.setMessage(R.string.dialog_remove_text);
					diag.setPositiveButton(android.R.string.yes, (dialog, which) -> new Thread(() -> {
						getContext().getContentResolver().delete(databaseUri, null, null);
						FileHandler.deleteStory(getContext(), story.getId());
					}).start());
					diag.setNegativeButton(android.R.string.no, null);
					diag.show();
					return true;

				case R.id.menu_library_context_author: {
					final Uri.Builder builder = new Builder();
					builder.scheme(getString(R.string.fanfiction_scheme))
							.authority(getString(R.string.fanfiction_authority))
							.appendEncodedPath("u")
							.appendEncodedPath(story.getAuthorId() + "")
							.appendEncodedPath("");
					final Intent i = new Intent(getContext(), AuthorMenuActivity.class);
					i.setData(builder.build());
					startActivity(i);
					return true;
				}
				case R.id.menu_library_context_view_reviews: {
					final Uri.Builder builder = new Builder();
					builder.scheme(getString(R.string.fanfiction_scheme))
							.authority(getString(R.string.fanfiction_authority))
							.appendEncodedPath("r")
							.appendEncodedPath(Long.toString(story.getId()))
							.appendEncodedPath("");
					final Intent i = new Intent(getContext(), ReviewMenuActivity.class);
					i.setData(builder.build());
					startActivity(i);
					return true;
				}

				default:
					return false;
			}
		}

		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			switch (id) {
				case LOADER_LIBRARY:
					mProgressBar.setVisibility(View.VISIBLE);
					mLoader = new LibraryLoader(getContext(), StoryProvider.FF_CONTENT_URI, GET_PROJECTION, args);
					return mLoader;
				default:
					return null;
			}
		}

		@Override
		public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
			inflater.inflate(R.menu.library_menu, menu);

			final SearchView searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.search));
			if (searchView == null) return;

			// Set the listener for Search events. Note that the editor action listener is used instead
			// of the onQueryTextListener in order to catch empty string submissions.
			final TextView searchViewTxt = (TextView) searchView.findViewById(R.id.search_src_text);
			searchViewTxt.setOnEditorActionListener((textView, i, keyEvent) -> {
				mLoader.onSearch(searchView.getQuery().toString());
				searchView.clearFocus();
				return false;
			});

			// If the close button is pressed, clear the search. This is required because the close
			// button won't trigger the onEditorAction listener
			final View closeBtn = searchView.findViewById(R.id.search_close_btn);
			closeBtn.setOnClickListener(view -> mLoader.onSearch(null));
		}

		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id) {
			StoryDisplayActivity.openStory(getContext(), id, Site.FANFICTION, false);
		}

		@Override
		public void onLoaderReset(Loader<Cursor> loader) {
			mAdapter.swapCursor(null);
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
			mLoader = (LibraryLoader) loader;
			mAdapter.swapCursor(data);

			mProgressBar.setVisibility(View.GONE);
			mEmptyView.setVisibility(View.GONE);
			mListView.setVisibility(View.VISIBLE);

			// Display the "No Stories Found" message if the list is empty
			if (mAdapter.isEmpty()) {
				mEmptyView.setVisibility(View.VISIBLE);
				mListView.setVisibility(View.GONE);
			}

			getActivity().supportInvalidateOptionsMenu();
		}

		public void onFilter(int[] selected) {
			mLoader.filter(selected);
		}

		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			switch (item.getItemId()) {
				case R.id.library_menu_sync_all:
					Cursor c = mAdapter.getCursor();
					int columnId = c.getColumnIndex(KEY_STORY_ID);
					int columnLast = c.getColumnIndex(KEY_LAST);
					int columnOffset = c.getColumnIndex(KEY_OFFSET);
					if (c.moveToFirst()) {
						do {
							final Uri.Builder storyUri = Sites.FANFICTION.BASE_URI.buildUpon();
							storyUri.appendPath("s");
							storyUri.appendPath(Long.toString(c.getLong(columnId)));
							storyUri.appendPath("");
							LibraryDownloader.download(getContext(), storyUri.build(), c.getInt(columnLast), c.getInt(columnOffset));
						} while (c.moveToNext());
					}
					return true;
				case R.id.filter:
					// Create and display a new filter
					mLoader.onFilterClick(getActivity());
					return true;
				case R.id.library_menu_add_by_id:
					downloadByIdDialog();
					return true;
				default:
					break;
			}
			return false;
		}

		@Override
		public void onPrepareOptionsMenu(Menu menu) {
			final MenuItem syncAll = menu.findItem(R.id.library_menu_sync_all);
			if (mAdapter.isEmpty()) {
				syncAll.setEnabled(false);
				syncAll.getIcon().setAlpha(64);
			} else {
				syncAll.setEnabled(true);
				syncAll.getIcon().setAlpha(255);
			}

			// Only disable the filter if the unfiltered list is empty
			final MenuItem filter = menu.findItem(R.id.filter);
			if (mLoader == null || (mAdapter.isEmpty() && !mLoader.isFilterAvailable())) {
				filter.setEnabled(false);
				filter.getIcon().setAlpha(64);
			} else {
				filter.setEnabled(true);
				filter.getIcon().setAlpha(255);
			}
		}

		@Override
		public void onSaveInstanceState(Bundle outState) {
			super.onSaveInstanceState(outState);
			mLoader.onSaveInstanceState(outState);
		}

		private void downloadByIdDialog() {
			// Create a dialog with a single editText field
			final AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
			alert.setTitle(R.string.diag_by_id_title);
			final EditText input = new EditText(getContext());
			input.setInputType(InputType.TYPE_CLASS_NUMBER);
			alert.setView(input);
			alert.setPositiveButton(R.string.diag_btn_pos, (dialog, whichButton) -> {
				final Editable value = input.getText();
				try {
					final long id = Long.parseLong(value.toString());
					final Builder storyUri = Sites.FANFICTION.BASE_URI.buildUpon();
					storyUri.appendPath("s");
					storyUri.appendPath(Long.toString(id));
					storyUri.appendPath("");
					LibraryDownloader.download(getContext(), storyUri.build(), 1, 0);
				} catch (Exception e) {
					Toast toast = Toast.makeText(getContext(), R.string.menu_library_by_id_error, Toast.LENGTH_SHORT);
					toast.show();
				}
			});
			alert.setNegativeButton(R.string.diag_btn_neg, (dialog, whichButton) -> {
				final Toast toast = Toast.makeText(getContext(), R.string.dialog_cancelled, Toast.LENGTH_SHORT);
				toast.show();
			});
			alert.show();
		}
	}
}
