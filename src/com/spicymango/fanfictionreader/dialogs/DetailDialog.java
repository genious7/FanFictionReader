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
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Shows the details menu
 * @author Michael Chen
 */
public class DetailDialog extends DialogFragment implements OnClickListener{

	public final static String EXTRA_STORY = "Map";
	public final static String EXTRA_AUTHOR = "AUTHOR";
	
	private Story mStory;
	private boolean mShowAuthor;
	
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
				R.string.detail_published,
				R.string.detail_complete
	};
	
	public DetailDialog(){
	}
	
	@SuppressLint("InflateParams")
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		super.onCreateDialog(savedInstanceState);
		
		mStory = getArguments().getParcelable(EXTRA_STORY);
		mShowAuthor = getArguments().getBoolean(EXTRA_AUTHOR);

		int vPad = getResources().getDimensionPixelSize(R.dimen.activity_vertical_margin);
		int hPad = getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin);
		
		final LinearLayout layout = new LinearLayout(getActivity());
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setPadding(hPad, vPad, hPad, vPad);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(mStory.getName());
		builder.setView(layout);
		
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
				DateFormat.getDateInstance().format(mStory.getPublished()),
				mStory.isCompleted() ? getString(R.string.complete_true) : getString(R.string.complete_false)
		};
		
		for (int i = 0; i < formats.length; i++) {
			if (stringValues[i].length() != 0) {
				TextView label = new TextView(getActivity());
				label.setText(Html.fromHtml(getString(formats[i], stringValues[i])));
				layout.addView(label);
			}
		}
		
		if (mShowAuthor) {
			Button authorBtn = new Button(getActivity());
			authorBtn.setText(R.string.menu_navigation_browse_by_author);
			authorBtn.setOnClickListener(this);
			authorBtn.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			layout.addView(authorBtn);
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
		DialogFragment diag = new DetailDialog();
		Bundle bundle = new Bundle();
		bundle.putParcelable(EXTRA_STORY, story);
		bundle.putBoolean(EXTRA_AUTHOR, showAuthor);
		diag.setArguments(bundle);
		diag.show(context.getSupportFragmentManager(), null);
	}
	
}
