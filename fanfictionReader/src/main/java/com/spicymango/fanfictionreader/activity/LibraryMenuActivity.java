package com.spicymango.fanfictionreader.activity;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputType;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.spicymango.fanfictionreader.LibraryDownloader;
import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.Settings;
import com.spicymango.fanfictionreader.activity.reader.StoryDisplayActivity;
import com.spicymango.fanfictionreader.dialogs.DetailDialog;
import com.spicymango.fanfictionreader.menu.authormenu.AuthorMenuActivity;
import com.spicymango.fanfictionreader.menu.reviewmenu.ReviewMenuActivity;
import com.spicymango.fanfictionreader.menu.storymenu.FilterDialog.FilterDialog;
import com.spicymango.fanfictionreader.menu.storymenu.FilterDialog.FilterDialog.FilterListener;
import com.spicymango.fanfictionreader.menu.storymenu.FilterDialog.SpinnerData;
import com.spicymango.fanfictionreader.provider.SqlConstants;
import com.spicymango.fanfictionreader.provider.StoryProvider;
import com.spicymango.fanfictionreader.util.FileHandler;
import com.spicymango.fanfictionreader.util.Story;

/**
 * An activity which displays the stories that are saved in the library.
 * @author Michael Chen
 */
public class LibraryMenuActivity extends AppCompatActivity implements LoaderCallbacks<Cursor>, SqlConstants, OnItemClickListener, FilterListener{
	private static final int LOADER_LIBRARY = 0;
	private static final int LOADER_FILTER = 1;
	
	private static final String STATE_FILTER = "STATE_FILTER";

	private static final int[] DEST_PROJECTION = {
			R.id.story_menu_list_item_title,
			R.id.story_menu_list_item_summary,
			R.id.story_menu_list_item_author,
			R.id.story_menu_list_item_chapters,
			R.id.story_menu_list_item_words,
			R.id.story_menu_list_item_follows,
			R.id.completitionBar };

	private static final String[] GET_PROJECTION = { KEY_STORY_ID, KEY_TITLE,
			KEY_SUMMARY, KEY_AUTHOR, KEY_CHAPTER, KEY_LENGTH, KEY_UPDATED,
			KEY_LAST, KEY_CATEGORY, KEY_OFFSET };
	
	private static final String[] TO_PROJECTION = { KEY_TITLE, KEY_SUMMARY,
			KEY_AUTHOR, KEY_CHAPTER, KEY_LENGTH, KEY_UPDATED, KEY_LAST };

	private ArrayList<SpinnerData> filterData;
	private SimpleCursorAdapter mAdapter;
	private ListView mListView;
	private View mProgressBar;
	private boolean mLoadedFandom;
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		// Inflate the basic menu
		getMenuInflater().inflate(R.menu.library_context_menu, menu);

		// Obtain the id that is being loaded
		final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		final long storyId = info.id;
		final Uri databaseUri = Uri.withAppendedPath(StoryProvider.FF_CONTENT_URI, String.valueOf(storyId));
		final Cursor c = getContentResolver().query(databaseUri, null, null, null, null);
		c.moveToFirst();
		final Story story = Story.fromCursor(c);
		c.close();

		// Gray out the review option if required.
		MenuItem reviewItem = menu.findItem(R.id.menu_library_context_view_reviews);
		reviewItem.setEnabled(story.getReviews() != 0);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		long id = info.id;
		
		final Uri databaseUri = Uri.withAppendedPath(StoryProvider.FF_CONTENT_URI, String.valueOf(id));
		
		Cursor c = getContentResolver().query(databaseUri, null, null, null, null);
		c.moveToFirst();
		final Story story = Story.fromCursor(c);
		c.close();

		switch (item.getItemId()) {
			case R.id.menu_library_context_details:
				DetailDialog.show(this, story);
				return true;

			case R.id.menu_library_context_delete:
				AlertDialog.Builder diag = new AlertDialog.Builder(this);
				diag.setTitle(R.string.dialog_remove);
				diag.setMessage(R.string.dialog_remove_text);
				diag.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						new Thread(new Runnable() {
							@Override
							public void run() {
								int length = story.getChapterLength();
								getContentResolver().delete(databaseUri, null, null);
								FileHandler.deleteStory(LibraryMenuActivity.this, story.getId());
							}
						}).start();
					}
				});
				diag.setNegativeButton(android.R.string.no, null);
				diag.show();
				return true;

			case R.id.menu_library_context_author: {
				Uri.Builder builder = new Builder();

				builder.scheme(getString(R.string.fanfiction_scheme))
						.authority(getString(R.string.fanfiction_authority))
						.appendEncodedPath("u")
						.appendEncodedPath(story.getAuthorId() + "")
						.appendEncodedPath("");
				Intent i = new Intent(this, AuthorMenuActivity.class);
				i.setData(builder.build());
				startActivity(i);
				return true;
			}
			case R.id.menu_library_context_view_reviews: {
				Uri.Builder builder = new Builder();
				builder.scheme(getString(R.string.fanfiction_scheme))
						.authority(getString(R.string.fanfiction_authority))
						.appendEncodedPath("r")
						.appendEncodedPath(Long.toString(story.getId()))
						.appendEncodedPath("");
				Intent i = new Intent(this, ReviewMenuActivity.class);
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
			return new LibraryLoader(this, StoryProvider.FF_CONTENT_URI, GET_PROJECTION, filterQuery(), null, sortOrder());
		case LOADER_FILTER:
			return new CursorLoader(this);
		default:
			return null;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.library_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id) {
		StoryDisplayActivity.openStory(this, id, Site.FANFICTION, false);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

		switch (loader.getId()) {
		case LOADER_LIBRARY:
			LibraryLoader l = (LibraryLoader) loader;
			mAdapter.swapCursor(data);
			mProgressBar.setVisibility(View.GONE);
			if (!mLoadedFandom) {
				prepareFilter(l.fandoms);
				mLoadedFandom = true;
			}
			supportInvalidateOptionsMenu();
			break;
		case LOADER_FILTER:

			break;

		default:
			break;
		}	
	}
	
	/**
	 * Processes the new filter, restarting the loader if any of the filters was changed.
	 * @param selected
	 */
	public void onFilter(int[] selected) {
		
		for (int i = 0; i < selected.length; i++) {
			filterData.get(i).setSelected(selected[i]);
		}
		
		getSupportLoaderManager().restartLoader(LOADER_LIBRARY, null, this);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			return true;
		case R.id.library_menu_sync_all:
			Cursor c = mAdapter.getCursor();
			int columnId = c.getColumnIndex(KEY_STORY_ID);
			int columnLast = c.getColumnIndex(KEY_LAST);
			int columnOffset = c.getColumnIndex(KEY_OFFSET);
			if (c.moveToFirst()) {
			    do {
			    	LibraryDownloader.download(this, c.getLong(columnId), c.getInt(columnLast), c.getInt(columnOffset));
			    } while (c.moveToNext());
			}
			return true;
		case R.id.filter:
			// Create and display a new filter
			FilterDialog.Builder builder = new FilterDialog.Builder();
			builder.addSingleSpinner(getString(R.string.filter_category), filterData.get(0));
			builder.addSingleSpinner(getString(R.string.filter_sort), filterData.get(1));
			builder.addSingleSpinner(getString(R.string.filter_length), filterData.get(2));
			builder.addSingleSpinner(getString(R.string.filter_type), filterData.get(3));
			builder.addSingleSpinner(getString(R.string.filter_status), filterData.get(4));
			builder.show(this);
			return true;
		case R.id.library_menu_add_by_id:
			downloadByIdDialog();
			return true;
		default:
			break;
		}
		return  false;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem syncAll = menu.findItem(R.id.library_menu_sync_all);
		if (mAdapter.isEmpty()) {
			syncAll.setEnabled(false);
			syncAll.getIcon().setAlpha(64);
		}else{
			syncAll.setEnabled(true);
			syncAll.getIcon().setAlpha(255);
		}
		
		MenuItem filter = menu.findItem(R.id.filter);
		if (mAdapter.isEmpty() && filterData == null) {
			filter.setEnabled(false);
			filter.getIcon().setAlpha(64);
		}else{
			filter.setEnabled(true);
			filter.getIcon().setAlpha(255);
		}
		
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		if (mLoadedFandom) {
			outState.putParcelableArrayList(STATE_FILTER, filterData);
		}
		super.onSaveInstanceState(outState);
	}
	
	private void downloadByIdDialog(){
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(R.string.diag_by_id_title);
		final EditText input = new EditText(this);
		input.setInputType(InputType.TYPE_CLASS_NUMBER);
		alert.setView(input);
		alert.setPositiveButton(R.string.diag_btn_pos, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				Editable value = input.getText();
				try {
					long id = Long.parseLong(value.toString());
					LibraryDownloader.download(LibraryMenuActivity.this, id, 1, 0);
				} catch (Exception e) {
					Toast toast = Toast.makeText(LibraryMenuActivity.this, R.string.menu_library_by_id_error, Toast.LENGTH_SHORT);
					toast.show();
				}
			}
		});
		alert.setNegativeButton(R.string.diag_btn_neg, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				Toast toast = Toast.makeText(LibraryMenuActivity.this, R.string.dialog_cancelled, Toast.LENGTH_SHORT);
				toast.show();
			}
		});
		alert.create();
		alert.show();
	}
	
	private String filterQuery(){

		// If the filter is unavailable, disregard
		if (filterData == null) return null;
	
		StringBuilder builder = new StringBuilder();
		
		builder.append("1 = 1");
		
		if (filterData.get(2).getSelected() != 0) {
			builder.append(" AND ");
			builder.append(filterData.get(2).getName() + filterData.get(2).getCurrentFilter());
		}
		
		if (filterData.get(3).getSelected() != 0) {
			builder.append(" AND ");
			builder.append(filterData.get(3).getName() + filterData.get(3).getCurrentFilter());
		}

		if (filterData.get(4).getSelected() != 0) {
			builder.append(" AND ");
			builder.append(filterData.get(4).getName() + filterData.get(4).getCurrentFilter());
		}
		
		if (filterData.get(0).getSelected() != 0) {
			String key = filterData.get(0).getCurrentFilter();
			key = key.replaceAll("'", "''");
			builder.append(" AND ");
			builder.append(filterData.get(0).getName() +" LIKE '%" + key + "%' ");
		}
		
		return builder.toString();
	}
	
	/**
	 * Initializes the filter
	 */
	private void initFilter(){
		filterData = new ArrayList<>();
		
		// Filter by fandom
		filterData.add(new SpinnerData(KEY_CATEGORY, new ArrayList<String>(), new ArrayList<String>(), 0));
		mLoadedFandom = false;
				
		// Filter by sort order
		String[] sortBy = getResources().getStringArray(R.array.menu_library_sort_by);
		String[] sortKey = {
				KEY_UPDATED + " DESC",
				KEY_PUBLISHED + " DESC",
				KEY_TITLE + " COLLATE NOCASE ASC",
				KEY_AUTHOR + " COLLATE NOCASE ASC",
				KEY_FAVORITES + " DESC",
				KEY_FOLLOWERS + " DESC",
				//We substract 1 from the last chapter and chapter so chapter 1 always starts at 0 and the last chapter always ends at 1
				//When the percentage is the same we will use the default sort behavior(update date)
				"(CAST(" + KEY_LAST + " - 1 AS FLOAT) / (" + KEY_CHAPTER + " - 1)) ASC, " + KEY_UPDATED + " DESC"
		};
		filterData.add(new SpinnerData("SortBy", sortBy, sortKey, 0));

		// Filter by words
		String[] words = getResources().getStringArray(R.array.menu_library_filter_words);
		String[] wordKey = { "", " < 1000", " < 5000", " > 1000", " > 5000", " > 10000", " > 20000", " > 40000",
				" > 60000", " > 100000" };
		filterData.add(new SpinnerData(KEY_LENGTH, words, wordKey, 0));

		// Filter by type (regular vs. crossover)
		String[] type = getResources().getStringArray(R.array.menu_library_filter_type);
		String[] typeKey = { "", " NOT LIKE '%Crossover' ", " LIKE '%Crossover' " };
		filterData.add(new SpinnerData(KEY_CATEGORY, type, typeKey, 0));

		// Filter if complete
		String[] status = getResources().getStringArray(R.array.menu_library_filter_status);
		String[] statusKey = { "", " != 0", " = 0" };
		filterData.add(new SpinnerData(KEY_COMPLETE, status, statusKey, 0));
	}

	/**
	 * Prepares the filter based on the available Fandoms.
	 * @param fandoms A set containing the available fandoms.
	 */
	private void prepareFilter(Set<String> fandoms) {
		List<String> fandomList = filterData.get(0).getLabels();
		List<String> fandomFilter = filterData.get(0).getFilters();
		fandomList.clear();
		fandomList.addAll(fandoms);
		fandomFilter.clear();		
		fandomFilter.addAll(fandoms);
	}

	/**
	 * Gets the SQL order by statement based on the currently selected filter.
	 * @return
	 */
	private String sortOrder(){
		return filterData.get(1).getCurrentFilter();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Settings.setOrientationAndTheme(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_list_view);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setTitle(R.string.menu_library_title);

		mListView = (ListView) findViewById(android.R.id.list);
		mListView.setOnItemClickListener(this);
		View footer = getLayoutInflater().inflate(R.layout.footer_list, mListView, false);
		mListView.addFooterView(footer, null, false);

		mAdapter = new SimpleCursorAdapter(this, R.layout.library_menu_list_item, null, TO_PROJECTION, DEST_PROJECTION,	0);
		mAdapter.setViewBinder(new LibraryBinder(this));
		mListView.setAdapter(mAdapter);
		registerForContextMenu(mListView);

		findViewById(R.id.story_load_pages).setVisibility(View.GONE);
		findViewById(R.id.row_retry).setVisibility(View.GONE);
		mProgressBar = findViewById(R.id.progress_bar);
		
		if (savedInstanceState != null && savedInstanceState.containsKey(STATE_FILTER)) {
			mLoadedFandom = true;
			filterData = savedInstanceState.getParcelableArrayList(STATE_FILTER);
		} else {
			initFilter();
		}

		getSupportLoaderManager().initLoader(LOADER_LIBRARY, null, this);
	}

	/**
	 * A ViewBinder that adds the prefixes to the stories' details.
	 * @author Michael Chen
	 *
	 */
	private static class LibraryBinder implements SimpleCursorAdapter.ViewBinder{
		/**
		 * Contains the templates for the stories' additional details
		 */
		private final String words, chapters;
		private final DateFormat format;
		private final int chapterColumn;
		
		/**
		 * Creates a new Library Binder using the current context. 
		 * @param context
		 */
		public LibraryBinder(Context context) {
			words = context.getString(R.string.menu_library_words);
			chapters = context.getString(R.string.menu_library_chapters);
			format = SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT);
			chapterColumn = Arrays.asList(GET_PROJECTION).indexOf(KEY_CHAPTER);
		}
		
		@Override
		public boolean setViewValue(View v, Cursor c, int column) {
			
			String text2;
			switch (v.getId()) {
			case R.id.story_menu_list_item_words:
				text2 = String.format(Locale.US, words, c.getInt(column));
				((TextView)v).setText(text2);
				return true;
			case R.id.story_menu_list_item_chapters:
				text2 = String.format(chapters, c.getInt(column));
				((TextView)v).setText(text2);
				return true;
			case R.id.story_menu_list_item_follows:
				text2 = format.format(c.getLong(column));
				((TextView)v).setText(text2);
				return true;
			case R.id.completitionBar:
				ProgressBar bar = (ProgressBar) v; 
				int max = c.getInt(chapterColumn);
				bar.setMax(max - 1);
				bar.setProgress(c.getInt(column) - 1);
				return true;
			default:
				return false;
			}
		}
		
	}
	
	private static class LibraryLoader extends CursorLoader{
		
		private Set<String> fandoms;
		
		public LibraryLoader(Context context, Uri uri, String[] projection,
				String selection, String[] selectionArgs, String sortOrder) {
			super(context, uri, projection, selection, selectionArgs, sortOrder);
		}

		@Override
		public Cursor loadInBackground() {

			Cursor c = super.loadInBackground();

			if (fandoms == null && c != null) {
				String all = getContext().getString(R.string.menu_library_filter_all);
				fandoms = new TreeSet<String>(new FilterComparator(all));
				fandoms.add(all);
				if (c.moveToFirst()) {
					int index = c.getColumnIndex(KEY_CATEGORY);
					do {
						String category = c.getString(index);
						int i = category.indexOf(" + ");
						int e = category.indexOf(" Crossover");
						if (i != -1 && e != -1) {
							String cat1 = category.substring(0, i);
							String cat2 = category.substring(i + 3, e);
							fandoms.add(cat1);
							fandoms.add(cat2);
						} else {
							fandoms.add(category);
						}
					} while (c.moveToNext());
				}
			}
			return c;
		}

		private static class FilterComparator implements Comparator<String> {

			private final String all;

			public FilterComparator(String all) {
				this.all = all;
			}

			@Override
			public int compare(String lhs, String rhs) {
				if (lhs.equals(all)) {
					return -1;
				} else if (rhs.equals(all)) {
					return 1;
				} else {
					return lhs.compareTo(rhs);
				}
			}
		}
	}
}
