package com.spicymango.fanfictionreader.activity;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.filter.FilterDialog;
import com.spicymango.fanfictionreader.menu.BaseActivity;
import com.spicymango.fanfictionreader.util.MenuObject;
import com.spicymango.fanfictionreader.util.SearchLoader;
import com.spicymango.fanfictionreader.util.Sites;

public class SearchAuthorActivity extends BaseActivity<MenuObject> implements OnQueryTextListener{
	private SearchLoader<MenuObject> mLoader; 
	private SearchView sView;
	
	@Override
	public Loader<List<MenuObject>> onCreateLoader(int id, Bundle args) {
		mLoader = new AuthorLoader(this, args);
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
		Intent i = new Intent(this, AuthorMenuActivity.class);
		Uri uri = Uri.parse(mList.get(position).getUri());
		i.setData(uri);
		startActivity(i);	
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view,
			int position, long id) {
		return false;
	}

	@Override
	public void onLoadFinished(Loader<List<MenuObject>> loader,
			List<MenuObject> data) {
		super.onLoadFinished(loader, data);
		mLoader = (SearchLoader<MenuObject>) loader;
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
	protected BaseAdapter getAdapter() {
		return new AuthorAdapter(this, mList);
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
	
	private static final class AuthorLoader extends SearchLoader<MenuObject>{
		String formatString;
		
		public AuthorLoader(Context context, Bundle savedInstanceState) {
			super(context, savedInstanceState);
			formatString = context.getString(R.string.menu_navigation_count_story);
		}

		@Override
		protected void resetFilter() {
			filter = new int[]{1,0,0,0,0,0,0,0,0,0,0,0,0,201};	
		}

		@Override
		protected Uri getUri(int currentPage) {
			Uri.Builder builder = Sites.FANFICTION.BASE_URI.buildUpon();
			builder.path("search.php")
					.appendQueryParameter("type", "author")
					.appendQueryParameter("ready", "1")
					.appendQueryParameter("keywords", mQuery)
					.appendQueryParameter("categoryid", filter[13] + "")
					.appendQueryParameter("genreid", filter[2] + "")
					.appendQueryParameter("languageid", filter[5] + "")
					.appendQueryParameter("ppage", currentPage + "")
					.appendQueryParameter("words", filter[6] + "");
			return builder.build();
		}

		@Override
		protected boolean load(Document document, List<MenuObject> list) {
			// Load the filters if they aren't already loaded.
			if (mFilterList == null) {
				mFilterList = SearchFilter(document);
			}	
			
			Elements authors = document.select("div#content div.bs");
			Element link;
			Element stories;
			
			if (authors.isEmpty()) {
				Element auth = document.select("div#content form a").first();
				if (auth != null) {
					Pattern pattern = Pattern.compile(":(\\d++)");
					Matcher matcher = pattern.matcher(auth.parent().ownText());
					if (matcher.find()) {
						String storyCount = String.format(formatString, matcher.group(1));
						list.add(new MenuObject(auth.text(), storyCount, auth.attr("abs:href"), 0));
					}		
				}
			}
			
			for (Element author : authors) {
				link = author.select("a[href^=/u/]").first();
				if (link == null) return false;

				String name = link.text();
				String uri = link.attr("abs:href");

				stories = author.select("span").first();
				if (stories == null) return false;

				String storyCount = String.format(formatString, stories.text());

				MenuObject object = new MenuObject(name, storyCount, uri, 0);
				list.add(object);
			}

			return true;
		}
		
	}
	
	private static class AuthorAdapter extends ArrayAdapter<MenuObject>{
			
		/**
		 * Initializes the adapter
		 * @param context The current context
		 * @param layoutResourceId The resource ID for a layout file
		 * @param data The objects to represent in the list view
		 */
		public AuthorAdapter(Context context, List<MenuObject> data) {
			super(context, R.layout.category_menu_list_item, data);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			MenuObject menuRow = getItem(position);
			MenuItemHolder holder = null;
			
			if(convertView == null)
	        {
	            LayoutInflater inflater = ((Activity) getContext()).getLayoutInflater();
	            convertView = inflater.inflate(R.layout.category_menu_list_item, parent, false);
	           
	            holder = new MenuItemHolder();
	            holder.txtTitle = (TextView)convertView.findViewById(android.R.id.text1);
	            holder.txtViews = (TextView)convertView.findViewById(android.R.id.text2);
	            convertView.setTag(holder);
	        }
	        else
	        {
	            holder = (MenuItemHolder)convertView.getTag();
	        }
	       
	        
	        holder.txtTitle.setText(menuRow.getTitle());
	        holder.txtViews.setText(menuRow.getViews());
	       
	        return convertView;
		}
		
		/**
		 * A cache of the TextViews. Provides a speed improvement.
		 * @author Michael Chen
		 */
		static class MenuItemHolder
	    {
	        TextView txtTitle;
	        TextView txtViews;
	    }
	}


}
