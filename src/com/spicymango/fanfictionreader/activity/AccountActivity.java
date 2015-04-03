package com.spicymango.fanfictionreader.activity;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Connection.Method;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.Settings;
import com.spicymango.fanfictionreader.activity.reader.StoryDisplayActivity;
import com.spicymango.fanfictionreader.util.AsyncPost;
import com.spicymango.fanfictionreader.util.Result;
import com.spicymango.fanfictionreader.util.Story;
import com.spicymango.fanfictionreader.util.TabListener;
import com.spicymango.fanfictionreader.util.adapters.StoryReducedAdapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBarActivity;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;

public class AccountActivity extends ActionBarActivity {
	private static final String STATE_TAB = "Tab selected";
	private ActionBar actionbar;
	
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Settings.setOrientationAndTheme(this);
		super.onCreate(savedInstanceState);

		// setup action bar tabs
		actionbar = getSupportActionBar();
		actionbar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		actionbar.setDisplayHomeAsUpEnabled(true);

		Bundle bundle = new Bundle();
		bundle.putInt(AccountFragment.EXTRA_LOADER_ID,
				AccountFragment.LOADER_FAVORITES);

		Tab tab = actionbar
				.newTab()
				.setText(R.string.menu_favs)
				.setTabListener(
						new TabListener(this, AccountFragment.class, bundle));
		actionbar.addTab(tab);

		bundle = new Bundle();
		bundle.putInt(AccountFragment.EXTRA_LOADER_ID,
				AccountFragment.LOADER_FOLLOWS);
		tab = actionbar
				.newTab()
				.setText(R.string.menu_follows)
				.setTabListener(
						new TabListener(this, AccountFragment.class, bundle, "favorites"));
		actionbar.addTab(tab);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		actionbar.setSelectedNavigationItem(savedInstanceState
				.getInt(STATE_TAB));
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putInt(STATE_TAB, actionbar.getSelectedNavigationIndex());
		super.onSaveInstanceState(outState);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			return true;
		case R.id.account_menu_log_out:
			LogInActivity.logOut(this);
			finish();
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.account_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	public static class AccountFragment extends Fragment implements
			OnItemClickListener, OnClickListener, LoaderCallbacks<Result> {
		private static final String EXTRA_LOADER_ID = "Loader ID";
		private static final int LOADER_FAVORITES = 0;
		private static final int LOADER_FOLLOWS = 1;

		private List<Story> mList;

		private BaseAdapter mAdapter;
		/**
		 * The retry button shown upon connection failure
		 */
		private View mNoConnectionBar;
		/**
		 * The progress spinner displayed while the loader loads
		 */
		private View mProgressBar;
		/**
		 * The button used to load the next page
		 */
		private Button mAddPageButton;

		private AccountLoader mLoader;

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {

			mList = new ArrayList<Story>();
			mAdapter = new StoryReducedAdapter(getActivity(), mList);

			View v = inflater.inflate(R.layout.activity_list_view, container,
					false);
			ListView listView = (ListView) v.findViewById(android.R.id.list);
			View footer = inflater.inflate(R.layout.footer_list, null);
			listView.addFooterView(footer, null, false);
			listView.setOnItemClickListener(this);
			listView.setAdapter(mAdapter);
			registerForContextMenu(listView);

			mAddPageButton = (Button) footer
					.findViewById(R.id.story_load_pages);
			mAddPageButton.setOnClickListener(this);
			mProgressBar = footer.findViewById(R.id.progress_bar);
			mNoConnectionBar = footer.findViewById(R.id.row_no_connection);
			View retryButton = mNoConnectionBar
					.findViewById(R.id.retry_internet_connection);
			retryButton.setOnClickListener(this);

			int loaderId = getArguments().getInt(EXTRA_LOADER_ID);
			getLoaderManager().initLoader(loaderId, savedInstanceState, this);

			return v;
		}
		
		@Override
		public void onCreateContextMenu(ContextMenu menu, View v,
				ContextMenuInfo menuInfo) {
			super.onCreateContextMenu(menu, v, menuInfo);
			getActivity().getMenuInflater().inflate(R.menu.account_context_menu, menu);
		}
		
		@Override
		public boolean onContextItemSelected(MenuItem item) {
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
			final long id = info.id;
			final int position = info.position;
			Story story = mList.get(position);
			
			switch (item.getItemId()) {
			case R.id.menu_library_context_delete:
				
				AlertDialog.Builder diag = new AlertDialog.Builder(
						getActivity());
				diag.setTitle(R.string.dialog_remove);
				diag.setMessage(R.string.dialog_remove_text);
				diag.setPositiveButton(android.R.string.yes,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,	int which) {
								
								HashMap<String, String> data = new HashMap<String, String>();
								data.put("rstoryid[]", Long.toString(id));
								data.put("action", "remove");
								data.put("sort", "adate");

								Uri.Builder uri = new Uri.Builder();
								uri.scheme(getString(R.string.fanfiction_scheme));
								uri.authority(getString(R.string.fanfiction_authority));
								uri.appendEncodedPath("m");

								if (mLoader instanceof FavoriteLoader) {
									uri.appendEncodedPath("f_story.php");
								} else {
									uri.appendEncodedPath("a_story.php");
								}

								new AsyncPost(getActivity(),
										R.string.toast_removed, data, uri
												.build(), Method.POST)
										.execute();
								
								mList.remove(position);	
								mAdapter.notifyDataSetChanged();
							}
				});
				diag.setNegativeButton(android.R.string.no, null);
				diag.show();

				return true;
			
			case R.id.menu_library_context_author:
				Uri.Builder builder = new Builder();

				builder.scheme(getString(R.string.fanfiction_scheme))
						.authority(getString(R.string.fanfiction_authority))
						.appendEncodedPath("u")
						.appendEncodedPath(story.getAuthor_id() + "")
						.appendEncodedPath("");
				Intent i = new Intent(getActivity(), AuthorMenuActivity.class);
				i.setData(builder.build());
				startActivity(i);
			default:
				return super.onContextItemSelected(item);
			}
		}

		@Override
		public Loader<Result> onCreateLoader(int id, Bundle args) {
			switch (id) {
			case LOADER_FAVORITES:
				return new FavoriteLoader(getActivity(), args);
			case LOADER_FOLLOWS:
				return new FollowsLoader(getActivity(), args);
			default:
				return null;
			}

		}

		@Override
		public void onLoadFinished(Loader<Result> loader, Result data) {
			mLoader = (AccountLoader) loader;
			switch (data) {
			case LOADING:
				mProgressBar.setVisibility(View.VISIBLE);
				mAddPageButton.setVisibility(View.GONE);
				mNoConnectionBar.setVisibility(View.GONE);
				break;
			case ERROR_CONNECTION:
				mProgressBar.setVisibility(View.GONE);
				mAddPageButton.setVisibility(View.GONE);
				mNoConnectionBar.setVisibility(View.VISIBLE);
				break;
			case ERROR_LOGIN:
				Intent i = new Intent(getActivity(), LogInActivity.class);
				startActivityForResult(i, 0);
				break;
			case SUCCESS:
				mProgressBar.setVisibility(View.GONE);
				mNoConnectionBar.setVisibility(View.GONE);
				mList.clear();
				mList.addAll(mLoader.mData);
				mAdapter.notifyDataSetChanged();
				//
				// ((ActionBarActivity)getActivity()).getSupportActionBar().setSubtitle(mLoader.mAuthor);
				//
				// if (mLoader.hasNextPage()) {
				// String text = String.format(
				// getString(R.string.menu_story_page_button),
				// mLoader.mCurrentPage + 1,
				// mLoader.mTotalPages);
				// mAddPageButton.setVisibility(View.VISIBLE);
				// mAddPageButton.setText(text);
				// } else {
				mAddPageButton.setVisibility(View.GONE);
				// }
				break;

			default:
				break;
			}
		}

		@Override
		public void onLoaderReset(Loader<Result> loader) {

		}

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.retry_internet_connection:
				mLoader.startLoading();
				break;

			default:
				break;
			}
		}

		@Override
		public void onSaveInstanceState(Bundle outState) {
			mLoader.onSaveInstanceState(outState);
			super.onSaveInstanceState(outState);
		}

		@Override
		public void onActivityResult(int requestCode, int resultCode,
				Intent data) {
			if (requestCode == 0) {
				if (resultCode == RESULT_OK) {
				mLoader.startLoading();
				}else{
					getActivity().finish();
				}
			}
			super.onActivityResult(requestCode, resultCode, data);
		}

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			StoryDisplayActivity.openStory(getActivity(), id, Site.FANFICTION, true);
		}

		private static final class FollowsLoader extends AccountLoader {

			public FollowsLoader(Context context, Bundle args) {
				super(context, args);
			}

			@Override
			protected String getUrl() {
				return "https://m.fanfiction.net/m/a_story.php";
			}

		}

		private static final class FavoriteLoader extends AccountLoader {

			public FavoriteLoader(Context context, Bundle args) {
				super(context, args);
			}

			@Override
			protected String getUrl() {
				return "https://m.fanfiction.net/m/f_story.php";
			}

		}

		private static abstract class AccountLoader extends
				AsyncTaskLoader<Result> {
			private static final String STATE_DATA = "mData";
			private static final String STATE_CHAGED = "mDataHasChanged";

			private ArrayList<Story> mData;
			private boolean mDataHasChanged;

			public AccountLoader(Context context, Bundle args) {
				super(context);
				if (args != null && args.containsKey(STATE_DATA)) {
					mData = args.getParcelableArrayList(STATE_DATA);
					mDataHasChanged = args.getBoolean(STATE_CHAGED);
				} else {
					mData = new ArrayList<Story>();
					mDataHasChanged = true;
				}

			}

			private void onSaveInstanceState(Bundle in) {
				in.putParcelableArrayList(STATE_DATA, mData);
				in.putBoolean(STATE_CHAGED, mDataHasChanged);
			}

			@Override
			public Result loadInBackground() {

				Map<String, String> cookies = LogInActivity
						.getCookies(getContext());
				if (cookies == null) {
					return Result.ERROR_LOGIN;
				}

				try {
					Document document = Jsoup.connect(getUrl())
							.cookies(cookies).timeout(10000).followRedirects(false).get();

					if (!document.select("body div.panel span.gui_warning")
							.isEmpty()) {
						return Result.ERROR_LOGIN;
					}

					if (!parse(document)) {
						return Result.ERROR_PARSE;
					}

					mDataHasChanged = false;
					return Result.SUCCESS;

				} catch (IOException e) {
					return Result.ERROR_CONNECTION;
				}
			}

			protected abstract String getUrl();

			private static final Pattern pattern = Pattern
					.compile("/[su]/([\\d]++)/");

			private boolean parse(Document document) {

				Elements stories = document
						.select("#gui_table1i > tbody > tr > td");

				Matcher storyIdMatcher = pattern.matcher("");
				Matcher authorIdMatcher = pattern.matcher("");

				SimpleDateFormat updatedFormatter = new SimpleDateFormat(
						"'u:'MM-dd-yyyy", Locale.US);
				SimpleDateFormat publishedFormatter = new SimpleDateFormat(
						"+MM-dd-yyyy", Locale.US);

				for (Element element : stories) {
					Elements links = element.select("a");
					Elements data = element.select(".gray");

					if (links.isEmpty() || data.isEmpty())
						continue;

					Element story = links.first();
					Element author = links.last();
					Element category = data.first();
					Element summary = data.last();

					storyIdMatcher.reset(story.attr("href"));
					authorIdMatcher.reset(author.attr("href"));

					if (!storyIdMatcher.find() || !authorIdMatcher.find()) {
						return false;
					}

					Date update, published;

					try {
						 update = updatedFormatter.parse(data.get(1).text());
						 published = publishedFormatter.parse(element.children().last()
								.text());
					} catch (ParseException e) {
						return false;
					}

					Story storyObj = new Story(Long.parseLong(storyIdMatcher
							.group(1)), story.text(), author.text(),
							Long.parseLong(authorIdMatcher.group(1)),
							summary.text(), category.text(), "", "", "", 0, 0,
							0, 0, update, published, false);

					mData.add(storyObj);

				}

				return true;
			}
			
			@Override
			protected void onStartLoading() {
				if (mDataHasChanged) {
					deliverResult(Result.LOADING);
					forceLoad();
				} else {
					deliverResult(Result.SUCCESS);
				}
			}

		}
	}
}
