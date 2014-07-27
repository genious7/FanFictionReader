/**
 * 
 */
package com.spicymango.fanfictionreader.activity;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.dialogs.DetailDialog;
import com.spicymango.fanfictionreader.util.BaseActivity;
import com.spicymango.fanfictionreader.util.BaseLoader;
import com.spicymango.fanfictionreader.util.Parser;
import com.spicymango.fanfictionreader.util.Story;
import com.spicymango.fanfictionreader.util.StoryMenuAdapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;

public class AuthorMenuActivity extends BaseActivity<Story>{

	/**
	 * The uri of the currently loaded page
	 */
	private Uri mUri;
	
	@Override
	public Loader<List<Story>> onCreateLoader(int id, Bundle args) {
		return new AuthorLoader(this, args, authorId(mUri));
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		Uri.Builder builder = new Uri.Builder();
		builder.scheme(getString(R.string.fanfiction_scheme))
				.authority(getString(R.string.fanfiction_authority))
				.appendPath("s")// Story
				.appendPath("" + id)// Id
				.appendPath("1")// Chapter 1
				.appendPath("");// Adds the '/'

		Intent i = new Intent(this, StoryDisplayActivity.class);
		i.setData(builder.build());
		startActivity(i);
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view,
			int position, long id) {
		DetailDialog.show(this, mList.get(position));
		return true;
	}

	@Override
	protected BaseAdapter getAdapter() {
		return new StoryMenuAdapter(this, mList);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mUri = getIntent().getData();
		getSupportLoaderManager().initLoader(0, savedInstanceState, this);
	}
	
	private static long authorId(Uri uri) {
		String segment = uri.getPathSegments().get(1);
		return Long.parseLong(segment);
	}
	
	@Override
	public void onLoadFinished(Loader<List<Story>> loader, List<Story> data) {
		super.onLoadFinished(loader, data);
		
		AuthorLoader tmpLoader = (AuthorLoader) loader;
		getSupportActionBar().setSubtitle(tmpLoader.mAuthor);
		
	}
	
	private static class AuthorLoader extends BaseLoader<Story>{
		public String mAuthor;
		private long mAuthorId;
		private static final String EXTRA_AUTHOR = "Extra author";
		
		public AuthorLoader(Context context, Bundle savedInstanceState, long authorId) {
			super(context, savedInstanceState);
			if (savedInstanceState != null) {
				mAuthor = savedInstanceState.getString(EXTRA_AUTHOR);
				mAuthor = mAuthor == null ? "" : mAuthor;
			}
			mAuthorId = authorId;
		}
		
		@Override
		public void onSavedInstanceState(Bundle outState) {
			outState.putString(EXTRA_AUTHOR, mAuthor);
			super.onSavedInstanceState(outState);
		}

		@Override
		protected Uri formatUri(int currentPage) {
			Uri.Builder builder = BASE_URI.buildUpon();
			builder.appendPath("u")
				.appendPath(mAuthorId + "")
				.appendPath("")
				.appendQueryParameter("a", "s")
				.appendQueryParameter("p", currentPage + "");
			return builder.build();
		}

		@Override
		protected int getTotalPages(Document document) {
			return Math.max(Parser.getpageNumber(document),getCurrentPage());
		}

		private static final  Pattern pattern = Pattern.compile(
		"/s/([\\d]++)/");
		
		@Override
		protected boolean load(Document document, List<Story> list) {
			mAuthor = getAuthor(document);
			
			Elements summaries = document.select("div#content div.bs");
			
			Matcher storyIdMatcher = pattern.matcher("");
			
			for (Element element : summaries) {
				element.select("b").unwrap();
				Element title = element.select("a[href~=(?i)/s/\\d+/1/.*]").first();
				Element attribs = element.select("div.gray").first();
				Elements dates = element.select("span[data-xutime]");
				
				storyIdMatcher.reset(title.attr("href"));
				
				storyIdMatcher.find();
					
				long updateDate = 0;
				long publishDate = 0;

				updateDate = Long.parseLong(dates.first().attr("data-xutime")) * 1000;
				publishDate = Long.parseLong(dates.last().attr("data-xutime")) * 1000;
				
				boolean complete;	
				Elements imgs = element.select("img.mm");
				complete = !imgs.isEmpty();

				Story TempStory = new Story(Long.parseLong(storyIdMatcher
						.group(1)), title.ownText(), mAuthor, mAuthorId,
						element.ownText().replaceFirst("(?i)by\\s*", ""),
						attribs.text(), updateDate, publishDate, complete);

				list.add(TempStory);
			}
			return true;
		}
		
		private String getAuthor(Document document) {
			Elements author = document.select("div#content div b");
			if (author.isEmpty()) {
				return "";
			} else {
				return author.first().ownText();
			}
		}	
	}	
}