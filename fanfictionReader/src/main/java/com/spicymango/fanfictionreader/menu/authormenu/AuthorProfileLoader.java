package com.spicymango.fanfictionreader.menu.authormenu;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.SpannedString;

import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.util.Parser;
import com.spicymango.fanfictionreader.util.Result;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

import androidx.loader.content.AsyncTaskLoader;

/**
 * Loads the author's biography Created by Michael Chen on 01/30/2016.
 */
class AuthorProfileLoader {

	static class FanFictionProfileLoader extends AsyncTaskLoader<Result> {
		private final static String STATE_DATA = "STATE CURRENT DATA PROFILE";
		private final static String STATE_CHANGED = "STATE CHANGED PROFILE";
		private static final String STATE_AUTHOR = "STATE AUTHOR PROFILE";


		public String mAuthor;
		private final long mAuthorId;
		public ArrayList<Spanned> mData;
		private boolean mDataHasChanged;
		private final Uri BASE_URI;

		private String mHtmlFromWebView;


		public FanFictionProfileLoader(Context context, Bundle args, long authorId) {
			super(context);

			mAuthorId = authorId;

			if (args != null && args.containsKey(STATE_DATA)) {
				mDataHasChanged = args.getBoolean(STATE_CHANGED, true);
				mAuthor = args.getString(STATE_AUTHOR);

				List<String> spans = args.getStringArrayList(STATE_DATA);
				mData = new ArrayList<>(spans.size());
				for (String string : spans) {
					mData.add(Html.fromHtml(string));
				}
			} else {
				mData = new ArrayList<>();
				mDataHasChanged = true;
			}

			// Generates the base Uri
			String scheme = context.getString(R.string.fanfiction_scheme);
			String authority = context.getString(R.string.fanfiction_authority);
			Uri.Builder builder = new Uri.Builder();
			builder.scheme(scheme);
			builder.authority(authority);
			BASE_URI = builder.build();

		}

		public final void setHtmlFromWebView(String html) {
			mHtmlFromWebView = html;
		}

		public void onSavedInstanceState(Bundle outState) {

			ArrayList<String> spans = new ArrayList<>();
			for (Spanned span : mData) {
				spans.add(Html.toHtml(span));
			}
			outState.putStringArrayList(STATE_DATA, spans);

			outState.putBoolean(STATE_CHANGED, mDataHasChanged);
			outState.putString(STATE_AUTHOR, mAuthor);
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

		@Override
		public Result loadInBackground() {
			if (mHtmlFromWebView == null) {
				return Result.ERROR_CLOUDFLARE_CAPTCHA;
			} else if (mHtmlFromWebView.equalsIgnoreCase("404")) {
				return Result.ERROR_CONNECTION;
			} else {
				final Document document = Jsoup.parse(mHtmlFromWebView, formatUri().toString());

				if (mAuthor == null) {
					mAuthor = getAuthor(document);
				}

				if (load(document, mData)) {
					mDataHasChanged = false;
					return Result.SUCCESS;
				} else {
					return Result.ERROR_PARSE;
				}
			}
		}

		@Override
		public void deliverResult(Result data) {
			super.deliverResult(data);
		}

		protected Uri formatUri() {
			Uri.Builder builder = BASE_URI.buildUpon();
			builder.appendPath("u").appendPath(mAuthorId + "")
					.appendPath("").appendQueryParameter("a", "b");
			return builder.build();
		}

		protected boolean load(Document document, List<Spanned> list) {

			Elements summaries = document.select("div#content > div");

			if (summaries.size() < 3) {
				SpannedString string = new SpannedString(getContext()
																 .getString(R.string.menu_author_no_profile));
				list.add(string);
				return true;
			}

			Element txtElement = summaries.get(2);
			Element dateElement = summaries.get(1);
			Spanned txt = Html.fromHtml(txtElement.html());
			Spanned date = Html.fromHtml(dateElement.html());
			list.addAll(Parser.split(txt));
			list.addAll(Parser.split(date));

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
