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
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.SearchView.OnQueryTextListener;
import android.text.TextUtils;
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

import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.menu.BaseActivity;
import com.spicymango.fanfictionreader.menu.BaseLoader.Filterable;
import com.spicymango.fanfictionreader.menu.authormenu.AuthorMenuActivity;
import com.spicymango.fanfictionreader.menu.storymenu.FilterDialog.FilterDialog;
import com.spicymango.fanfictionreader.menu.storymenu.FilterDialog.SpinnerData;
import com.spicymango.fanfictionreader.menu.storymenu.FilterDialog.FilterDialog.FilterListener;
import com.spicymango.fanfictionreader.util.MenuObject;
import com.spicymango.fanfictionreader.util.SearchLoader;
import com.spicymango.fanfictionreader.util.Sites;

public class SearchAuthorActivity extends BaseActivity<MenuObject> implements OnQueryTextListener, FilterListener{
	private SearchLoader<MenuObject> mLoader; 
	private SearchView sView;
	
	@NonNull
	@Override
	public Loader<List<MenuObject>> onCreateLoader(int id, Bundle args) {
		mLoader = new AuthorLoader(this, args);
		super.mLoader = mLoader;
		return mLoader;
	}

	
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
	public void onLoadFinished(@NonNull Loader<List<MenuObject>> loader,
							   List<MenuObject> data) {
		super.onLoadFinished(loader, data);
		mLoader = (SearchLoader<MenuObject>) loader;
		supportInvalidateOptionsMenu();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.filter){
			((Filterable) mLoader).onFilterClick(this);
			return true;
		}{
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem filter = menu.findItem(R.id.filter);
		if (mLoader == null || !((Filterable)mLoader).isFilterAvailable()) {
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
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		LoaderManager.getInstance(this).initLoader(0, savedInstanceState, this);
	}
	
	private static final class AuthorLoader extends SearchLoader<MenuObject> implements Filterable{
		private final String formatString;
		
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
			// Don't load anything on empty queries.
			if (TextUtils.isEmpty(mQuery)) return null;

			Uri.Builder builder = Sites.FANFICTION.BASE_URI.buildUpon();
			builder.path("search/")
					.appendQueryParameter("type", "author")
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
		protected boolean load(Document document, List<MenuObject> list) {
			// Load the filters if they aren't already loaded.
			if (!isFilterAvailable()) {
				mFilterData = SearchFilter(document);
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
			builder.show((SearchAuthorActivity) activity);
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
	
	private static class AuthorAdapter extends ArrayAdapter<MenuObject>{
			
		/**
		 * Initializes the adapter
		 * @param context The current context
		 * @param data The objects to represent in the list view
		 */
		public AuthorAdapter(Context context, List<MenuObject> data) {
			super(context, R.layout.category_menu_list_item, data);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			MenuObject menuRow = getItem(position);
			MenuItemHolder holder;
			
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

	@Override
	public void onFilter(int[] selected) {
		((Filterable) mLoader).filter(selected);
	}


}
