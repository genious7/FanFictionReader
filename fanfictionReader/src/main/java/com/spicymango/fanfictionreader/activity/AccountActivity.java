package com.spicymango.fanfictionreader.activity;

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
import com.spicymango.fanfictionreader.activity.reader.StoryDisplayActivity;
import com.spicymango.fanfictionreader.menu.CloudflareFragment;
import com.spicymango.fanfictionreader.menu.TabActivity;
import com.spicymango.fanfictionreader.menu.authormenu.AuthorMenuActivity;
import com.spicymango.fanfictionreader.util.AsyncPost;
import com.spicymango.fanfictionreader.util.Result;
import com.spicymango.fanfictionreader.util.Story;
import com.spicymango.fanfictionreader.util.adapters.StoryReducedAdapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;
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

public class AccountActivity extends TabActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle bundle = new Bundle();
		bundle.putInt(AccountFragment.EXTRA_LOADER_ID,
				AccountFragment.LOADER_FAVORITES);

		addTab(R.string.menu_favs, AccountFragment.class, bundle);

		bundle = new Bundle();
		bundle.putInt(AccountFragment.EXTRA_LOADER_ID,
					  AccountFragment.LOADER_FOLLOWS);
		addTab(R.string.menu_follows, AccountFragment.class, bundle);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final int itemId = item.getItemId();
		if (itemId == R.id.account_menu_log_out){
			LogInActivity.logOut(this);
			finish();
			return true;
		} else{
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

		final ActivityResultLauncher<Intent> mLauncher = registerForActivityResult(
				new ActivityResultContracts.StartActivityForResult(), result -> {
			if (result.getResultCode() == RESULT_OK) {
				mLoader.startLoading();
			}else{
				requireActivity().finish();
			}
		});

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {

			mList = new ArrayList<>();
			mAdapter = new StoryReducedAdapter(getActivity(), mList);

			View v = inflater.inflate(R.layout.activity_list_view, container,
					false);
			ListView listView = v.findViewById(android.R.id.list);
			View footer = inflater.inflate(R.layout.footer_list, null);
			listView.addFooterView(footer, null, false);
			listView.setOnItemClickListener(this);
			listView.setAdapter(mAdapter);
			registerForContextMenu(listView);

			mAddPageButton = footer
					.findViewById(R.id.story_load_pages);
			mAddPageButton.setOnClickListener(this);
			mProgressBar = footer.findViewById(R.id.progress_bar);
			mNoConnectionBar = footer.findViewById(R.id.row_retry);
			View retryButton = mNoConnectionBar
					.findViewById(R.id.btn_retry);
			retryButton.setOnClickListener(this);

			assert getArguments() != null;
			int loaderId = getArguments().getInt(EXTRA_LOADER_ID);
			LoaderManager.getInstance(this).initLoader(loaderId, savedInstanceState, this);

			return v;
		}
		
		@Override
		public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v,
										ContextMenuInfo menuInfo) {
			super.onCreateContextMenu(menu, v, menuInfo);
			requireActivity().getMenuInflater().inflate(R.menu.account_context_menu, menu);
		}
		
		@Override
		public boolean onContextItemSelected(MenuItem item) {
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
			final long id = info.id;
			final int position = info.position;
			Story story = mList.get(position);

			if (item.getItemId() == R.id.menu_library_context_delete){
				AlertDialog.Builder diag = new AlertDialog.Builder(
						getActivity());
				diag.setTitle(R.string.dialog_remove);
				diag.setMessage(R.string.dialog_remove_text);
				diag.setPositiveButton(android.R.string.yes,
									   (dialog, which) -> {

										   HashMap<String, String> data = new HashMap<>();
										   data.put("rstoryid[]", Long.toString(id));
										   data.put("action", "remove");
										   data.put("sort", "adate");

										   Builder uri = new Builder();
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
									   });
				diag.setNegativeButton(android.R.string.no, null);
				diag.show();
				return true;
			} else if(item.getItemId() == R.id.menu_library_context_delete){
				Uri.Builder builder = new Builder();

				builder.scheme(getString(R.string.fanfiction_scheme))
						.authority(getString(R.string.fanfiction_authority))
						.appendEncodedPath("u")
						.appendEncodedPath(story.getAuthorId() + "")
						.appendEncodedPath("");
				Intent i = new Intent(getActivity(), AuthorMenuActivity.class);
				i.setData(builder.build());
				startActivity(i);
				return true;
			} else{
				return super.onContextItemSelected(item);
			}
		}

		@NonNull
		@Override
		public Loader<Result> onCreateLoader(int id, Bundle args) {
			switch (id) {
			case LOADER_FAVORITES:
				return new FavoriteLoader(getActivity(), args);
			case LOADER_FOLLOWS:
			default:
				return new FollowsLoader(getActivity(), args);
			}

		}

		@Override
		public void onLoadFinished(@NonNull Loader<Result> loader, Result data) {
			mLoader = (AccountLoader) loader;
			switch (data) {
			case LOADING:
				mProgressBar.setVisibility(View.VISIBLE);
				mAddPageButton.setVisibility(View.GONE);
				mNoConnectionBar.setVisibility(View.GONE);
				break;
			case ERROR_CLOUDFLARE_CAPTCHA:
				mProgressBar.setVisibility(View.VISIBLE);
				mAddPageButton.setVisibility(View.GONE);
				mNoConnectionBar.setVisibility(View.GONE);

				// Launch a new fragment
				final Uri uri = Uri.parse(mLoader.getUrl());
				final Bundle arguments = new Bundle();
				arguments.putParcelable(CloudflareFragment.EXTRA_URI, uri);

				final FragmentManager manager = getParentFragmentManager();
				manager.setFragmentResultListener("DATA_CLOUDFLARE",this,(requestKey, bundle) ->{
					mLoader.setHtmlFromWebView(bundle.getString("DATA"));
					mLoader.startLoading();
				});

				manager.beginTransaction()
						.add(CloudflareFragment.class, arguments, "DATA_CLOUDFLARE")
						.setReorderingAllowed(true)
						.commit();
				break;
			case ERROR_CONNECTION:
				mProgressBar.setVisibility(View.GONE);
				mAddPageButton.setVisibility(View.GONE);
				mNoConnectionBar.setVisibility(View.VISIBLE);
				break;
			case ERROR_LOGIN:
				final Intent i = new Intent(getActivity(), LogInActivity.class);
				mLauncher.launch(i);
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
		public void onLoaderReset(@NonNull Loader<Result> loader) {

		}

		@Override
		public void onClick(View v) {
			if (v.getId() == R.id.btn_retry){
				mLoader.startLoading();
			}
		}

		@Override
		public void onSaveInstanceState(@NonNull Bundle outState) {
			mLoader.onSaveInstanceState(outState);
			super.onSaveInstanceState(outState);
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
			private static final String STATE_CHANGED = "mDataHasChanged";

			private final ArrayList<Story> mData;
			private boolean mDataHasChanged;
			private String mHtmlFromWebView;

			public AccountLoader(Context context, Bundle args) {
				super(context);
				if (args != null && args.containsKey(STATE_DATA)) {
					mData = args.getParcelableArrayList(STATE_DATA);
					mDataHasChanged = args.getBoolean(STATE_CHANGED);
				} else {
					mData = new ArrayList<>();
					mDataHasChanged = true;
				}

			}

			public void setHtmlFromWebView(String data) {
				mHtmlFromWebView = data;
			}

			private void onSaveInstanceState(Bundle in) {
				in.putParcelableArrayList(STATE_DATA, mData);
				in.putBoolean(STATE_CHANGED, mDataHasChanged);
			}

			@Override
			public Result loadInBackground() {

				Map<String, String> cookies = LogInActivity
						.getCookies(getContext());
				if (cookies == null) {
					return Result.ERROR_LOGIN;
				}

				if (mHtmlFromWebView == null) {
					// Trigger the Cloudflare loading
					return Result.ERROR_CLOUDFLARE_CAPTCHA;
				} else if (mHtmlFromWebView.equalsIgnoreCase("404")){
					return Result.ERROR_CONNECTION;
				} else {
					final Document document = Jsoup.parse(mHtmlFromWebView, getUrl());
					if (!document.select("body div.panel span.gui_warning")
							.isEmpty()) {
						mHtmlFromWebView = null;
						return Result.ERROR_LOGIN;
					} else if (!parse(document)) {
						mHtmlFromWebView = null;
						return Result.ERROR_PARSE;
					}
				}

				mDataHasChanged = false;
				return Result.SUCCESS;
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
					
					Story.Builder builder = new Story.Builder();
					builder.setId((Long.parseLong(storyIdMatcher.group(1))));
					builder.setName(story.text());
					builder.setAuthor(author.text());
					builder.setAuthorId(Long.parseLong(authorIdMatcher.group(1)));
					builder.setSummary(summary.ownText().replaceFirst("^\\s*-?\\s*", ""));
					builder.setCategory(category.text());
					builder.setUpdateDate(update);
					builder.setPublishDate(published);
					builder.setCompleted(false);

					mData.add(builder.build());

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
