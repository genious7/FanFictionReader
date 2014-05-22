package com.spicymango.fanfictionreader;

import java.text.DateFormat;

import com.spicymango.fanfictionreader.util.Story;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

/**
 * Shows the details menu
 * @author Michael Chen
 *
 */
public class DetailDisplay extends Activity {
	public final static String MAP = "Map";
	
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
		
		Story values = (Story) getIntent().getParcelableExtra(MAP);
		this.setTitle(values.getName());
		
		
		
		String[] stringValues = {
				values.getAuthor(),
				values.getCategory(),
				values.getRating(),
				values.getlanguage(),
				values.getGenre(),
				String.valueOf(values.getChapterLenght()),
				values.getWordLenght(),
				values.getFavorites(),
				values.getFollows(),
				DateFormat.getDateInstance().format(values.getUpdated()),
				DateFormat.getDateInstance().format(values.getPublished())
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
}
