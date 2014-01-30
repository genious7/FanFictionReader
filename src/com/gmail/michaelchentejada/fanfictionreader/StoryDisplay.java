package com.gmail.michaelchentejada.fanfictionreader;

import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
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
	protected static final String CHAPTERS = "Chapters";
	protected static final String TITLE = "Title";
	protected static final String URL = "URL";
	
	private static final String CURRENTPAGE = "Current Page";
	private static final String CURRENTSTORY = "Story";
	
	private int currentPage = 1;
	private int onSavePage = 1;
	private int totalPages = 0;
	private Context context;
	private TextView story;
	
	private Button first;
	private Button selectPage;
	private Button prev;
	private Button next;
	private Button last;
	private ScrollView scrollview;
	
	private Spanned Story;
	private ParseStory parseStory = new ParseStory();
	
	private OnClickListener changePage  = new OnClickListener() {

		@Override
		public void onClick(View v) {
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
	};

	
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
		
		first.setOnClickListener(changePage);
		prev.setOnClickListener(changePage);
		next.setOnClickListener(changePage);
		last.setOnClickListener(changePage);
		selectPage.setOnClickListener(pageSelector);
		
		TextView title = (TextView)findViewById(R.id.read_story_title);
		title.setText(getIntent().getStringExtra(TITLE));
		
		story = (TextView)findViewById(R.id.read_story_text);
		story.setTextSize(TypedValue.COMPLEX_UNIT_SP, Settings.fontSize(context));
		scrollview = (ScrollView)findViewById(R.id.read_story_scrollview);
		
		totalPages = Integer.valueOf(getIntent().getStringExtra(CHAPTERS));

		if (savedInstanceState == null) {
			parseStory.execute();
		
		}else{
			onSavePage = savedInstanceState.getInt(CURRENTPAGE);
			currentPage = onSavePage;
			Story = (Spanned) savedInstanceState.getCharSequence(CURRENTSTORY);
			story.setText(Story, BufferType.SPANNABLE);
			selectPage.setText(currentPage + "/" + totalPages);
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
			String url = "https://m.fanfiction.net" + getIntent().getStringExtra(URL) + currentPage + "/";
			
			try {
				return Html.fromHtml(Parser.storyHTML(url));
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
				scrollview.fullScroll(ScrollView.FOCUS_UP);
				
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
}
