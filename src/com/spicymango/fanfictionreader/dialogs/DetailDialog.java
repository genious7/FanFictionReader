package com.spicymango.fanfictionreader.dialogs;

import java.text.DateFormat;

import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.activity.AuthorMenuActivity;
import com.spicymango.fanfictionreader.activity.LibraryMenuActivity;
import com.spicymango.fanfictionreader.util.Story;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

/**
 * Shows the details menu
 * @author Michael Chen
 */
public class DetailDialog extends DialogFragment implements OnClickListener{

	public final static String EXTRA_STORY = "Map";
	public final static String EXTRA_AUTHOR = "AUTHOR";
	
	private final Story mStory;
	private final boolean mShowAuthor;
	
	private final static int[] formats = {
				R.string.detail_author,
				R.string.detail_category,
				R.string.detail_rating,
				R.string.detail_languague,
				R.string.detail_genre,
				R.string.detail_chapters,
				R.string.detail_words,
				R.string.detail_favorites,
				R.string.detail_follows,
				R.string.detail_updated,
				R.string.detail_published
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
	
	public DetailDialog(Story story, boolean showAuthor){
		mStory = story;
		mShowAuthor = showAuthor;
	}
	
	@SuppressLint("InflateParams")
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View view = inflater.inflate(R.layout.activity_detail_view, null);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(mStory.getName());
		builder.setView(view);
		
		View btnAuthor = view.findViewById(R.id.btn_author);
		if (mShowAuthor) {
			btnAuthor.setOnClickListener(this);
		}else{
			btnAuthor.setVisibility(View.GONE);
			
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
		
		for (int i = 0; i < labels.length; i++) {
			TextView label = (TextView) view.findViewById(labels[i]);
			if (stringValues[i].equals("")) {
				label.setVisibility(View.GONE);
			}else{
				label.setText(Html.fromHtml(getString(formats[i], stringValues[i])));
			}
		}
		
		AlertDialog diag= builder.create();
		diag.setCanceledOnTouchOutside(true);
		return diag;
	}
	
	@Override
	public void onClick(View v) {
		Uri.Builder builder = new Builder();

		builder.scheme(getString(R.string.fanfiction_scheme))
				.authority(getString(R.string.fanfiction_authority))
				.appendEncodedPath("u")
				.appendEncodedPath(mStory.getAuthor_id() + "")
				.appendEncodedPath("");
		Intent i = new Intent(getActivity(), AuthorMenuActivity.class);
		i.setData(builder.build());
		startActivity(i);
		
	}
	
	public static final void show(ActionBarActivity context, Story story) {
		
		boolean showAuthor = true;
		
		if (context instanceof AuthorMenuActivity || context instanceof LibraryMenuActivity) {
			showAuthor = false;
		}
		
		DialogFragment diag = new DetailDialog(story, showAuthor);
		diag.show(context.getSupportFragmentManager(), null);
	}
	
}
