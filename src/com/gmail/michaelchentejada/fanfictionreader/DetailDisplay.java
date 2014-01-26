package com.gmail.michaelchentejada.fanfictionreader;

import java.util.HashMap;

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
	protected final static String MAP = "Map";
	
	private final static int[] textviews = {
				R.id.detail_author,
				R.id.detail_crossover,
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
				R.id.detail_crossover_label,
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
	
	private final static String[] hashMapKeys = {
			Parser.AUTHOR,
			Parser.CROSSOVER,
			Parser.CATEGORY,
			Parser.RATING,
			Parser.LANGUAGUE,
			Parser.GENRE,
			Parser.CHAPTER,
			Parser.LENGHT,
			Parser.FAVORITES,
			Parser.FOLLOWS,
			Parser.UPDATED,
			Parser.PUBLISHED
		};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_detail_view);
		setResult(RESULT_OK);
		
		@SuppressWarnings("unchecked")
		HashMap<String, String> values = (HashMap<String, String>) getIntent().getSerializableExtra(MAP);
		this.setTitle(values.get(Parser.TITLE));
		
		for (int i = 0; i < textviews.length; i++) {	
			if (values.get(hashMapKeys[i]).equals("0")) {
				((TextView)findViewById(textviews[i])).setVisibility(View.GONE);
				((TextView)findViewById(labels[i])).setVisibility(View.GONE);
			}else{
				((TextView)findViewById(textviews[i])).setText(values.get(hashMapKeys[i]));
			}
		}	
	}	
}
