package com.spicymango.fanfictionreader.activity;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.util.BaseActivity;
import com.spicymango.fanfictionreader.util.BaseLoader;
import com.spicymango.fanfictionreader.util.Parser;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.TextView;


public class CommunityMenuActivity extends BaseActivity<CommunityItem>{

	private CommunityLoader mLoader;
	
	@Override
	public Loader<List<CommunityItem>> onCreateLoader(int id, Bundle args) {
		mLoader = new CommunityLoader(this, args, getIntent().getData());
		return mLoader;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.community_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		Intent i = new Intent(this,StoryMenuActivity.class);
		Uri.Builder builder = BaseUri.buildUpon();
		builder.encodedPath(mList.get((int)id).uri);
		i.setData(builder.build());
		startActivity(i);	
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view,
			int position, long id) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onLoadFinished(Loader<List<CommunityItem>> loader,
			List<CommunityItem> data) {
		super.onLoadFinished(loader, data);
		mLoader = (CommunityLoader) loader;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportLoaderManager().initLoader(0, savedInstanceState, this);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		SortBy temp = SortBy.get(item.getItemId());
		if (temp == null) {
			return super.onOptionsItemSelected(item);
		}else{
			mLoader.setSort(temp);
			return true;
		}	
	}
	
	@Override
	protected BaseAdapter getAdapter() {
		return new CommunityAdapter(this, mList);
	}
	
	private static class CommunityLoader extends BaseLoader<CommunityItem>{
		private static final String STATE_SORT_BY = "Community Sort By";
		
		private Uri baseUri;
		private SortBy mSortBy;
		
		@Override
		public void onSavedInstanceState(Bundle outState) {
			outState.putSerializable(STATE_SORT_BY, mSortBy);
			super.onSavedInstanceState(outState);
		}

		public CommunityLoader(Context context, Bundle params, Uri uri) {
			super(context, params);
			if (params == null || !params.containsKey(STATE_SORT_BY)) {
				mSortBy = SortBy.FOLLOWS;
			}else{
				mSortBy = (SortBy) params.getSerializable(STATE_SORT_BY);
			}
			baseUri = uri;
			
		}

		@Override
		protected Uri formatUri(int currentPage) {
			Uri.Builder builder = baseUri.buildUpon();
			builder.appendPath("0")
			.appendPath(mSortBy.getId() + "")
			.appendPath(currentPage + "")
			.appendPath("");
			return builder.build();
		}

		@Override
		protected int getTotalPages(Document document) {
			 return Parser.Pages(document);
		}
		
		public void setSort(SortBy sortBy){
			if (mSortBy != sortBy) {
				mSortBy = sortBy;
				resetState();
				startLoading();
			}
		}

		@Override
		protected boolean load(Document document, List<CommunityItem> list) {
			
			Elements base = document.select("div#content > div.bs");
			Elements title = base.select("a");
			Elements summary = base.select("div.z-padtop");
			
			String communityText = getContext().getString(R.string.menu_navigation_count_story);
			
			final String dateFormat = "MM-dd-yy";
			final DateFormat format = new SimpleDateFormat(dateFormat, Locale.US);
			
			for (int i = 0; i < base.size(); i++) {
				String titleField = title.get(i).ownText();
				String url = title.get(i).attr("href");

				String n_views = title.get(i).child(0).ownText().replaceAll("[() ]", "");
				n_views = String.format(communityText, n_views);
				
				String summaryField = summary.get(i).ownText();
				
				String attrib[] = summary.get(i).child(0).ownText().toString().split("\\s+-\\s+");
				String languague = attrib[0];
				int staff = Integer.parseInt(attrib[1].replaceAll("[\\D]", ""));
				int follows = Integer.parseInt(attrib[2].replaceAll("[\\D]", ""));
				
				Date date;
				try {
					date = format.parse(attrib[3].replaceAll("(?i)since:\\s*", ""));
				} catch (ParseException e) {
					e.printStackTrace();
					date = new Date();
				}

				String author = attrib[4].replaceAll("(?i)founder:\\s*", "");
				
				list.add(new CommunityItem(titleField, url, author, summaryField, n_views, languague, staff, follows, date));
			}			
			return true;
		}
		
	}
	
	private enum SortBy {
		RANDOM(99, R.id.community_sort_random), 
		STAFF(1, R.id.community_sort_staff),
		STORIES(2, R.id.community_sort_stories),
		FOLLOWS(3, R.id.community_sort_follows), 
		CREATE_DATE(4, R.id.community_sort_date);

		public static final SortBy get(int res) {
			for (SortBy e : values()) {
				if (res == e.res) {
					return e;
				}
			}
			return null;
		}

		private final int id;

		private final int res;

		private SortBy(int id, int res) {
			this.id = id;
			this.res = res;
		}

		public int getId() {
			return id;
		}

	}
	
	private static final class CommunityAdapter extends ArrayAdapter<CommunityItem>{
	
	
	public CommunityAdapter(Context context,
			List<CommunityItem> objects) {
		super(context, R.layout.community_menu_list_item, objects);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if (convertView == null) {
			LayoutInflater inflater = ((Activity) getContext())
					.getLayoutInflater();
			convertView = inflater.inflate(R.layout.community_menu_list_item,
					parent, false);
			holder = new ViewHolder();
			holder.title = (TextView) convertView
					.findViewById(R.id.community_title);
			holder.summary = (TextView) convertView
					.findViewById(R.id.community_summary);
			holder.author = (TextView) convertView
					.findViewById(R.id.community_author);
			holder.stories = (TextView) convertView
					.findViewById(R.id.community_stories);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		holder.title.setText(getItem(position).title);
		holder.summary.setText(getItem(position).summary);
		holder.author.setText(getItem(position).author);
		holder.stories.setText(String.valueOf(getItem(position).stories));

		return convertView;
	}

	/**
	 * A small helper class to hold the id's of the views
	 * @author Michael Chen
	 */
	private static final class ViewHolder{
		private TextView author;
		private TextView title;
		private TextView stories;
		private TextView summary;
	}
}
	
}

/**
 * Contains the parameters that define a community.
 * 
 * @author Michael Chen
 */
final class CommunityItem implements Parcelable {
	protected String title;
	protected String uri;
	protected String author;
	protected String summary;
	protected String stories;
	private String languague;
	private int staff;
	private int follows;
	private Date published;

	public static final Parcelable.Creator<CommunityItem> CREATOR = new Creator<CommunityItem>() { // NO_UCD (unused code)

		@Override
		public CommunityItem createFromParcel(Parcel source) {
			return new CommunityItem(source);
		}

		@Override
		public CommunityItem[] newArray(int size) {
			return new CommunityItem[size];
		}
	};

	/**
	 * A constructor used for creating a community item from a parcel.
	 * 
	 * @param in
	 */
	private CommunityItem(Parcel in) {
		title = in.readString();
		uri = in.readString();
		author = in.readString();
		summary = in.readString();
		stories = in.readString();
		languague = in.readString();
		staff = in.readInt();
		follows = in.readInt();
		published = new Date(in.readLong());
	}

	public CommunityItem(String title, String uri, String author,
			String summary, String stories, String languague, int staff,
			int follows, Date published) {
		super();
		this.title = title;
		this.uri = uri;
		this.author = author;
		this.summary = summary;
		this.stories = stories;
		this.languague = languague;
		this.staff = staff;
		this.follows = follows;
		this.published = published;
	}

	/**
	 * Always returns 0
	 */
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(title);
		dest.writeString(uri);
		dest.writeString(author);
		dest.writeString(summary);
		dest.writeString(stories);
		dest.writeString(languague);
		dest.writeInt(staff);
		dest.writeInt(follows);
		dest.writeLong(published.getTime());
	}
}