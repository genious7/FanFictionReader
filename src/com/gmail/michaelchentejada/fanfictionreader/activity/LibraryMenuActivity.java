package com.gmail.michaelchentejada.fanfictionreader.activity;

import java.io.File;

import com.gmail.michaelchentejada.fanfictionreader.DetailDisplay;
import com.gmail.michaelchentejada.fanfictionreader.LibraryDownloader;
import com.gmail.michaelchentejada.fanfictionreader.R;
import com.gmail.michaelchentejada.fanfictionreader.Settings;
import com.gmail.michaelchentejada.fanfictionreader.util.SqlConstants;
import com.gmail.michaelchentejada.fanfictionreader.util.Story;
import com.gmail.michaelchentejada.fanfictionreader.util.StoryProvider;
import com.gmail.michaelchentejada.fanfictionreader.util.DatabaseHelper;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBarActivity;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
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
	
	/* (non-Javadoc)
	 * @see android.support.v7.app.ActionBarActivity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_list_view);
		getSupportLoaderManager().initLoader(LOADER_LIBRARY, null, this);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		Settings.setOrientation(this);
			
		mListView = (ListView)findViewById(R.id.list);
		mListView.setOnItemClickListener(this);
		mAdapter = new SimpleCursorAdapter(this, R.layout.story_menu_list_item, null, TO_PROJECTION, DEST_PROJECTION, 0);
		mListView.setAdapter(mAdapter);
		registerForContextMenu(mListView);
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		long id = info.id;
		
		DatabaseHelper db = new DatabaseHelper(this);
		Story story = db.getStory(id);
		db.close();
		
		switch (item.getItemId()) {
		
		case R.id.menu_library_context_details:
			Intent i = new Intent(this ,DetailDisplay.class);
			i.putExtra(DetailDisplay.MAP,story);
			startActivity(i);
			return true;
			
		case R.id.menu_library_context_delete:
			int length = story.getChapterLenght();
			for (int j = 0; j < length; j++) {
				File file = new File(getFilesDir(), story.getId() + "_" + j + ".txt");
				file.delete();
			}
			
			getContentResolver().delete(Uri.withAppendedPath(StoryProvider.CONTENT_URI, String.valueOf(id)), null, null);
			
			return true;
		default:
			return false;
		}
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
		if (mAdapter.isEmpty()) {
			menu.findItem(R.id.library_menu_sync_all).setEnabled(false);
		}else{
			menu.findItem(R.id.library_menu_sync_all).setEnabled(true);
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
			    	Intent i = new Intent(this, LibraryDownloader.class);
					i.putExtra(LibraryDownloader.EXTRA_STORY_ID, c.getFloat(columnId));
					i.putExtra(LibraryDownloader.EXTRA_LAST_PAGE, c.getInt(columnLast));
					startService(i);
			    } while (c.moveToNext());
			}
			return true;
		default:
			break;
		}
		return  false;
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
			return new CursorLoader(this, StoryProvider.CONTENT_URI, GET_PROJECTION, null, null, KEY_UPDATED);
		default:
			return null;
		}
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		mAdapter.changeCursor(data);
		supportInvalidateOptionsMenu();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.changeCursor(null);
	}
	
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id) {
		DatabaseHelper db = new DatabaseHelper(this);
		int lastPageRead = db.getLastChapterRead(id);
		if (lastPageRead == -1) {
			lastPageRead = 1;
		}
		Intent i = new Intent (this, StoryDisplayActivity.class);
		i.setData(Uri.fromFile(new File(getFilesDir(), id + "_" + lastPageRead + ".txt")));
		startActivity(i);
	}

}
