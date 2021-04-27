package com.spicymango.fanfictionreader.activity;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

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
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.menu.BaseActivity;
import com.spicymango.fanfictionreader.menu.BaseLoader.Filterable;
import com.spicymango.fanfictionreader.menu.communitymenu.CommunityMenuItem;
import com.spicymango.fanfictionreader.menu.communitymenu.CommunityAdapter;
import com.spicymango.fanfictionreader.menu.storymenu.FilterDialog.FilterDialog;
import com.spicymango.fanfictionreader.menu.storymenu.FilterDialog.SpinnerData;
import com.spicymango.fanfictionreader.menu.storymenu.FilterDialog.FilterDialog.FilterListener;
import com.spicymango.fanfictionreader.menu.storymenu.StoryMenuActivity;
import com.spicymango.fanfictionreader.util.SearchLoader;
import com.spicymango.fanfictionreader.util.Sites;

public class SearchCommunityActivity extends BaseActivity<CommunityMenuItem> implements OnQueryTextListener, FilterListener{
    private SearchLoader<CommunityMenuItem> mLoader;
    private SearchView sView;

    @NonNull
    @Override
    public Loader<List<CommunityMenuItem>> onCreateLoader(int id, Bundle args) {
        mLoader = new CommunityLoader(this, args);
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
        Intent i = new Intent(this, StoryMenuActivity.class);
        Uri uri = mList.get(position).getUri();
        i.setData(uri);
        startActivity(i);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view,
                                   int position, long id) {
        return false;
    }

    @Override
    public void onLoadFinished(@NonNull Loader<List<CommunityMenuItem>> loader,
							   List<CommunityMenuItem> data) {
        super.onLoadFinished(loader, data);
        mLoader = (SearchLoader<CommunityMenuItem>) loader;
        supportInvalidateOptionsMenu();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.filter){
            ((Filterable) mLoader).onFilterClick(this);
            return true;
        } else{
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
        return new CommunityAdapter(this, mList);
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

    private static final class CommunityLoader extends SearchLoader<CommunityMenuItem> implements Filterable{

        public CommunityLoader(Context context, Bundle savedInstanceState) {
            super(context, savedInstanceState);
        }

        @Override
        protected void resetFilter() {
            filter = new int[]{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
        }

        @Override
        protected Uri getUri(int currentPage) {
            // Don't load anything on empty queries.
            if (TextUtils.isEmpty(mQuery)) return null;

            Uri.Builder builder = Sites.FANFICTION.DESKTOP_URI.buildUpon();
            builder.path("search/")
                    .appendQueryParameter("type", "community")
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
        protected boolean load(Document document, List<CommunityMenuItem> list) {
            // Load the filters if they aren't already loaded.
            if (!isFilterAvailable()) {
                mFilterData = SearchFilter(document);
            }

            final Elements communities = document.select("div.z-list");
            final String counterFormat = getContext().getString(R.string.menu_navigation_count_story);
            final DateFormat dateFormat = new SimpleDateFormat("MM-dd-yy", Locale.US);

            for (Element community : communities) {
                // Get the title and the URL
                final Element titleElement = community.select("a").first();
                if (titleElement == null) return false;
                String title = titleElement.text();
                Uri url = Uri.parse(titleElement.absUrl("href"));

                final Element descElement = community.select("div.z-padtop").first();
                if (descElement == null) return false;

                // Get the attributes
                final Element attribElement = descElement.select("div.z-padtop2").first();
                if (attribElement == null) return false;
                // Split the attributes on the dash, removing whitespace at
                // either side
                String[] attribs = attribElement.ownText().split("\\s+-\\s+");

                // Parse the attributes
                String languague = attribs[1];
                // No need to check for NumberFormatException, regex ensures
                // only digits get through
                int staff = Integer.parseInt(attribs[2].replaceAll("[\\D]", ""));
                int stories = Integer.parseInt(attribs[3].replaceAll("[\\D]", ""));
                String nViews = String.format(counterFormat, stories);
                int follows = Integer.parseInt(attribs[4].replaceAll("[\\D]", ""));

                Date date;
                try {
                    date = dateFormat.parse(attribs[5].replaceAll("(?i)since:\\s*", ""));
                } catch (ParseException e) {
                    Log.e("FanFicCommunityLoader", e.getMessage());
                    return false;
                }

                String author = attribs[6].replaceAll("(?i)founder:\\s*", "");

                // Get the description
                attribElement.remove();
                String desc = descElement.text();

                list.add(new CommunityMenuItem(title, url, author, desc, nViews, languague, staff, follows, date));
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
            builder.show((SearchCommunityActivity) activity);
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

    @Override
    public void onFilter(int[] selected) {
        ((Filterable) mLoader).filter(selected);
    }
}
