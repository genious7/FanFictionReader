package com.gmail.michaelchentejada.fanfictionreader;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView.BufferType;

public class StoryDisplay extends Activity {
	private static final String TITLE = "Title";
	private static final String CURRENTPAGE = "Current Page";
	private static final String CURRENTSTORY = "Story";
	private static final String TOTALPAGE = "Total Pages";
	
	private int currentPage = 1;
	private int onSavePage = 1;
	private int totalPages = 0;
	private Context context;
	private TextView story;
	private String storyTitle = "";
	
	private Button first;
	private Button selectPage;
	private Button prev;
	private Button next;
	private Button last;
	private ScrollView scrollview;
	private int storyId;
	
	private Spanned Story;
	private ParseStory parseStory = new ParseStory();
	
	/**
	 * Handles the Buttons that change pages.
	 * @param v The view identifying the button clicked.
	 */
	public void changePage(View v) {
		switch (v.getId()) {
		case R.id.read_story_first:	
			currentPage = 1;
			break;
		case R.id.read_story_prev:
			currentPage = onSavePage -1;
			break;
		case R.id.read_story_next:
			currentPage = onSavePage + 1;
			break;
		case R.id.read_story_last:
			currentPage = totalPages;
			break;
		}
		
		parseStory.cancel(true);
		parseStory = new ParseStory();
		parseStory.execute();
	}

	private OnClickListener pageSelector = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			String[] Chapters = new String[totalPages];
			for (int i = 0; i < totalPages; i++) {
				Chapters[i] = getResources().getString(R.string.read_story_chapter) + (i+1);
			}
			builder.setItems(Chapters, new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int which) {
			           currentPage = which + 1;
			           parseStory.cancel(true);
			           parseStory = new ParseStory();
			           parseStory.execute();
			       }
			});
			builder.setInverseBackgroundForced(true);
			builder.create();
			builder.show();
		}
	};
	
	private OnClickListener addTolibrary = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			Intent i = new Intent(context, LibraryDownloader.class);
			i.putExtra(LibraryDownloader.EXTRA_STORY_ID, storyId);
			startService(i);			
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_read_story);
		context = this;
		
		first = (Button)findViewById(R.id.read_story_first);
		prev = (Button)findViewById(R.id.read_story_prev);
		next = (Button)findViewById(R.id.read_story_next);
		last = (Button)findViewById(R.id.read_story_last);
		selectPage = (Button)findViewById(R.id.read_story_page_counter);
		Button addtoLib = (Button)findViewById(R.id.read_story_add);
		addtoLib.setOnClickListener(addTolibrary);
		
		selectPage.setOnClickListener(pageSelector);
			
		story = (TextView)findViewById(R.id.read_story_text);
		story.setTextSize(TypedValue.COMPLEX_UNIT_SP, Settings.fontSize(context));
		scrollview = (ScrollView)findViewById(R.id.read_story_scrollview);
		
		parseURL(getIntent().getDataString());

		if (savedInstanceState == null) {
			parseStory.execute();
		
		}else{
			onSavePage = savedInstanceState.getInt(CURRENTPAGE);
			totalPages = savedInstanceState.getInt(TOTALPAGE);
			currentPage = onSavePage;
			Story = (Spanned) savedInstanceState.getCharSequence(CURRENTSTORY);
			story.setText(Story, BufferType.SPANNABLE);
			selectPage.setText(currentPage + "/" + totalPages);
			
			storyTitle = savedInstanceState.getString(TOTALPAGE);
			TextView title = (TextView)findViewById(R.id.read_story_title);
			title.setText(storyTitle);
			
			if (totalPages == currentPage) {
				next.setEnabled(false);
				last.setEnabled(false);
			}
			if (currentPage == 1) {
				prev.setEnabled(false);
				first.setEnabled(false);
			}
		}
	}
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putInt(CURRENTPAGE, onSavePage);
		outState.putCharSequence(CURRENTSTORY, Story);
		outState.putInt(TOTALPAGE,totalPages);
		outState.putString(TITLE, storyTitle);
		super.onSaveInstanceState(outState);
	}
	

	private class ParseStory extends AsyncTask<Void, Void, Spanned>{
		private ProgressDialog progressDialog; //Progress dialogue for the async task.
		@Override
		protected void onPreExecute() {
			
			progressDialog = new ProgressDialog(context);
			progressDialog.setTitle("");
			progressDialog.setMessage(getResources().getString(R.string.dialog_loading));
			progressDialog.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					if (currentPage == 1) {
						finish();
					}	else{
						cancel(true);
					}
				}
			});
			progressDialog.show();
			super.onPreExecute();
		}
		@Override
		protected Spanned doInBackground(Void...voids) {
			try {
				org.jsoup.nodes.Document document = Jsoup.connect("https://m.fanfiction.net/s/" + storyId + "/" + currentPage + "/").get();
				
				if (totalPages==0){
					storyTitle = document.select("div#content div b").first().ownText();
					
					Element link = document.select("body#top > div[align=center] > a").first();
					
					if (link != null)
						totalPages = Math.max(pageNumber(link.attr("href"),2),currentPage);
					else
						totalPages = 1;
				}
				
				Elements titles = document.select("div#storycontent");
				return Html.fromHtml(titles.html());
			} catch (IOException e) {
				return null;
			}
		}
		@Override
		
		protected void onPostExecute(Spanned result) {
			progressDialog.dismiss();
			if (result != null) {
				Story = result;
				onSavePage = currentPage;
				
				story.setText(result, BufferType.SPANNABLE);

				scrollview.post(new Runnable() {
					@Override
					public void run() {
						scrollview.fullScroll(View.FOCUS_UP);
					}
				});

				
				TextView title = (TextView)findViewById(R.id.read_story_title);
				title.setText(storyTitle);
				
				selectPage.setText(currentPage + "/" + totalPages);
				
				if (totalPages == currentPage) {
					next.setEnabled(false);
					last.setEnabled(false);
				}else{
					next.setEnabled(true);
					last.setEnabled(true);
				}
				if (currentPage == 1) {
					prev.setEnabled(false);
					first.setEnabled(false);
				}else {
					prev.setEnabled(true);
					first.setEnabled(true);
				}
			}else{
				if (currentPage == 1 && onSavePage == 1) {
					finish();
				}
				Toast toast = Toast.makeText(context, getResources().getString(R.string.dialog_internet), Toast.LENGTH_SHORT);
				toast.show();
			}
		}
	}
	
	@Override
	protected void onDestroy() {
		parseStory.cancel(true);
		super.onDestroy();
	}
	
	/**
	 * Parses the input url in order to obtain the page number and story id
	 * @param Url
	 */
	private void parseURL(String Url){
		storyId = pageNumber(Url, 1);
		currentPage = pageNumber(Url,2);
	}
	
	/**
	 * Extracts the page number or the story id from a url
	 * @param url The string containing the url that needs to be parsed
	 * @param group One for the story id, two for the page number.
	 * @return Either the story id or the page number
	 */
	private int pageNumber(String url, int group){
		final Pattern currentPageNumber = Pattern.compile("(?:/s/(\\d{1,10}+)/)(\\d++)(?:/)");
		Matcher matcher = currentPageNumber.matcher(url);
		if (matcher.find()) {
			return Integer.valueOf(matcher.group(group));
		}else{
			return 1;
		}
	}
}
