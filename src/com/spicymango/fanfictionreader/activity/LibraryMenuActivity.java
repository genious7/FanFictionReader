package com.spicymango.fanfictionreader.activity;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import com.spicymango.fanfictionreader.DetailDisplay;
import com.spicymango.fanfictionreader.LibraryDownloader;
import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.Settings;
import com.spicymango.fanfictionreader.filter.FilterDialog;
import com.spicymango.fanfictionreader.provider.SqlConstants;
import com.spicymango.fanfictionreader.provider.StoryProvider;
import com.spicymango.fanfictionreader.util.Story;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.InputType;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

/**
 * An activity which displays the stories that are saved in the library.
 * @author Michael Chen
 */
public class LibraryMenuActivity extends ActionBarActivity implements LoaderCallbacks<Cursor>, SqlConstants, OnItemClickListener{
	private static final int LOADER_LIBRARY = 0;
	private static final String[] GET_PROJECTION = {KEY_STORY_ID,KEY_TITLE,KEY_SUMMARY,KEY_AUTHOR,KEY_CHAPTER, KEY_LENGHT,KEY_FOLLOWERS, KEY_LAST};
	private static final String[] TO_PROJECTION = {KEY_TITLE,KEY_SUMMARY,KEY_AUTHOR,KEY_CHAPTER, KEY_LENGHT,KEY_FOLLOWERS};
	private static final int[] DEST_PROJECTION = {
			R.id.story_menu_list_item_title,
			R.id.story_menu_list_item_summary,
			R.id.story_menu_list_item_author,
			R.id.story_menu_list_item_chapters,
			R.id.story_menu_list_item_words,
			R.id.story_menu_list_item_follows};
	
	private SimpleCursorAdapter mAdapter;
	private ListView mListView;
	private int[] filter = {0,0,0,0,0,0,0,0,0,0,0,0,0,0};
	private ArrayList<Map<String, Integer>> filterList = new ArrayList<Map<String,Integer>>();
	private View mProgressBar;
	
	/* (non-Javadoc)
	 * @see android.support.v7.app.ActionBarActivity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_list_view);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		Settings.setOrientation(this);
			
		initFilter();
		
		mListView = (ListView)findViewById(R.id.list);
		mListView.setOnItemClickListener(this);
		View footer = getLayoutInflater().inflate(R.layout.progress_bar, null);
		mListView.addFooterView(footer, null, false);
		
		mAdapter = new SimpleCursorAdapter(this, R.layout.story_menu_list_item, null, TO_PROJECTION, DEST_PROJECTION, 0);
		mAdapter.setViewBinder(new LibraryBinder(this));
		mListView.setAdapter(mAdapter);
		registerForContextMenu(mListView);

		findViewById(R.id.story_load_pages).setVisibility(View.GONE);
		findViewById(R.id.row_no_connection).setVisibility(View.GONE);
		mProgressBar = findViewById(R.id.progress_bar); 
	
		getSupportLoaderManager().initLoader(LOADER_LIBRARY, null, this);
	}
	
	private void initFilter(){
		for (int i = 0; i < filter.length; i++) {
			filterList.add(new LinkedHashMap<String, Integer>());
		}
		
		String[] sortBy = getResources().getStringArray(R.array.menu_library_sort_by);
		for (int i = 0; i < sortBy.length; i++) {
			filterList.get(0).put(sortBy[i], i);
		}	
		
		String[] words = getResources().getStringArray(R.array.menu_library_filter_words);
		for (int i = 0; i < words.length; i++) {
			filterList.get(6).put(words[i], i);
		}	
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		long id = info.id;
		
		Uri databaseUri = Uri.withAppendedPath(StoryProvider.CONTENT_URI, String.valueOf(id));
		
		Cursor c = getContentResolver().query(databaseUri, null, null, null, null);
		c.moveToFirst();
		Story story = new Story(c);
		c.close();
		
		switch (item.getItemId()) {
		case R.id.menu_library_context_details:
			
			Intent e = new Intent(this ,DetailDisplay.class);
			e.putExtra(DetailDisplay.MAP,story);
			startActivity(e);
			return true;
			
		case R.id.menu_library_context_delete:
			int length = story.getChapterLenght();
			for (int j = 0; j < length; j++) {
				File file = new File(getFilesDir(), story.getId() + "_" + j + ".txt");
				file.delete();
			}
			
			getContentResolver().delete(databaseUri, null, null);
			
			return true;
		default:
			return false;
		}
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
				if (filter != this.filter) {
					this.filter = filter;
					getSupportLoaderManager().restartLoader(LOADER_LIBRARY, null, this);
				}
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu, android.view.View, android.view.ContextMenu.ContextMenuInfo)
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		getMenuInflater().inflate(R.menu.library_context_menu, menu);
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
		if (mAdapter.isEmpty()) {
			filter.setEnabled(false);
			filter.getIcon().setAlpha(64);
		}else{
			filter.setEnabled(true);
			filter.getIcon().setAlpha(255);
		}
		
		return super.onPrepareOptionsMenu(menu);
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
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
			if (c.moveToFirst()) {
			    do {
			    	LibraryDownloader.download(this, c.getLong(columnId), c.getInt(columnLast));
			    } while (c.moveToNext());
			}
			return true;
		case R.id.filter:
			FilterDialog.show(this, filterList, filter);
			return true;
		case R.id.library_menu_add_by_id:
			downloadByIdDialog();
			return true;
		default:
			break;
		}
		return  false;
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
					LibraryDownloader.download(LibraryMenuActivity.this, id, 1);
				} catch (Exception e) {
					Toast toast = Toast.makeText(LibraryMenuActivity.this, R.string.dialog_unspecified, Toast.LENGTH_SHORT);
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

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.library_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		switch (id) {
		case LOADER_LIBRARY:
			mProgressBar.setVisibility(View.VISIBLE);
			return new CursorLoader(this, StoryProvider.CONTENT_URI, GET_PROJECTION, filterQuery(), null, sortOrder());
		default:
			return null;
		}
	}
	
	private String filterQuery(){
		StringBuilder builder = new StringBuilder();
		
		switch (filter[6]) {
		case 1:
			builder.append(KEY_LENGHT + " < 1000");
			break;
		case 2:
			builder.append(KEY_LENGHT + " < 5000");
			break;
		case 3:
			builder.append(KEY_LENGHT + " > 1000");
			break;
		case 4:
			builder.append(KEY_LENGHT + " > 5000");
			break;
		case 5:
			builder.append(KEY_LENGHT + " > 10000");
			break;
		case 6:
			builder.append(KEY_LENGHT + " > 20000");
			break;
		case 7:
			builder.append(KEY_LENGHT + " > 40000");
			break;
		case 8:
			builder.append(KEY_LENGHT + " > 60000");
			break;
		case 9:
			builder.append(KEY_LENGHT + " > 100000");
			break;

		default:
			break;
		}
		
		return builder.toString();
	}
	
	private String sortOrder(){
		switch (filter[0]) {
		case 1:
			return KEY_PUBLISHED + " DESC";
		case 2:
			return KEY_TITLE + " COLLATE NOCASE ASC";
		case 3:
			return KEY_AUTHOR + " COLLATE NOCASE ASC";
		case 4:
			return KEY_FAVORITES + " DESC";
		case 5:
			return KEY_FOLLOWERS + " DESC";
		case 0: default:
			return KEY_UPDATED + " DESC";
		}	
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		mAdapter.swapCursor(data);
		mProgressBar.setVisibility(View.GONE);
		supportInvalidateOptionsMenu();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}
	
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id) {
		
		int lastPageRead = StoryProvider.lastChapterRead(this, id);
		
		Intent i = new Intent (this, StoryDisplayActivity.class);
		i.setData(Uri.parse("https://m.fanfiction.net/s/" + id + "/" + lastPageRead + "/"));
		startActivity(i);
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
		private final String words, chapters, follows;
		
		/**
		 * Creates a new Library Binder using the current context. 
		 * @param context
		 */
		public LibraryBinder(Context context) {
			words = context.getString(R.string.menu_library_words);
			chapters = context.getString(R.string.menu_library_chapters);
			follows = context.getString(R.string.menu_library_follows);		
		}
		
		@Override
		public boolean setViewValue(View v, Cursor c, int column) {
			String text2;
			switch (v.getId()) {
			case R.id.story_menu_list_item_words:
				text2 = String.format(Locale.US, words, c.getInt(column));
				break;
			case R.id.story_menu_list_item_chapters:
				text2 = String.format(chapters, c.getInt(column));
				break;
			case R.id.story_menu_list_item_follows:
				text2 = String.format(Locale.US, follows, c.getInt(column));
				break;
			default:
				return false;
			}
			((TextView)v).setText(text2);
			return true;
		}
		
	}
}
