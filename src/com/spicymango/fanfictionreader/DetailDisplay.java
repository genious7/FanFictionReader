package com.spicymango.fanfictionreader;

import java.text.DateFormat;

import com.spicymango.fanfictionreader.activity.AuthorMenuActivity;
import com.spicymango.fanfictionreader.util.Story;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

/**
 * Shows the details menu
 * @author Michael Chen
 *
 */
public class DetailDisplay extends Activity implements OnClickListener {
	public final static String MAP = "Map";
	public final static String EXTRA_AUTHOR = "AUTHOR";
	
	private Story mStory;
	
	private final static int[] textviews = {
				R.id.detail_author,
				R.id.detail_category,
				R.id.detail_rating,
				R.id.detail_languague,
				R.id.detail_genre,
				R.id.detail_chapters,
				R.id.detail_words,
				R.id.detail_favorites,
				R.id.detail_follows,
				R.id.detail_updated,
				R.id.detail_published
		};
	
	private final static int[] labels = {
				R.id.detail_author_label,
				R.id.detail_category_label,
				R.id.detail_rating_label,
				R.id.detail_languague_label,
				R.id.detail_genre_label,
				R.id.detail_chapters_label,
				R.id.detail_words_label,
				R.id.detail_favorites_label,
				R.id.detail_follows_label,
				R.id.detail_updated_label,
				R.id.detail_published_label
		};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_detail_view);
		setResult(RESULT_OK);
		
		mStory = (Story) getIntent().getParcelableExtra(MAP);
		this.setTitle(mStory.getName());
		
		View btnAuthor = findViewById(R.id.btn_author);
		if (getIntent().getBooleanExtra(EXTRA_AUTHOR, false)) {
			btnAuthor.setVisibility(View.GONE);
		}else{
			btnAuthor.setOnClickListener(this);
		}
		
		String[] stringValues = {
				mStory.getAuthor(),
				mStory.getCategory(),
				mStory.getRating(),
				mStory.getlanguage(),
				mStory.getGenre(),
				String.valueOf(mStory.getChapterLenght()),
				mStory.getWordLenght(),
				mStory.getFavorites(),
				mStory.getFollows(),
				DateFormat.getDateInstance().format(mStory.getUpdated()),
				DateFormat.getDateInstance().format(mStory.getPublished())
			};
		
		for (int i = 0; i < textviews.length; i++) {	
			if (stringValues[i].equals("")) {
				((TextView)findViewById(textviews[i])).setVisibility(View.GONE);
				((TextView)findViewById(labels[i])).setVisibility(View.GONE);
			}else{
				((TextView)findViewById(textviews[i])).setText(stringValues[i]);
			}
		}	
	}

	@Override
	public void onClick(View v) {
		Uri.Builder builder = new Builder();
		
		builder.scheme(getString(R.string.fanfiction_scheme))
		.authority(getString(R.string.fanfiction_authority))
		.appendEncodedPath("u")
		.appendEncodedPath(mStory.getAuthor_id() + "")
		.appendEncodedPath("");
		
		Intent i = new Intent(this,AuthorMenuActivity.class);
		i.setData(builder.build());
		startActivity(i);
	}	
}
