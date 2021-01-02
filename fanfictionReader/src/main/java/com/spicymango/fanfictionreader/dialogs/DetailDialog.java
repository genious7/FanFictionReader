package com.spicymango.fanfictionreader.dialogs;

import java.text.DateFormat;
import java.util.Date;

import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.menu.librarymenu.LibraryMenuActivity;
import com.spicymango.fanfictionreader.menu.authormenu.AuthorMenuActivity;
import com.spicymango.fanfictionreader.menu.reviewmenu.ReviewMenuActivity;
import com.spicymango.fanfictionreader.util.Parser;
import com.spicymango.fanfictionreader.util.Story;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Shows the details menu
 * @author Michael Chen
 */
public class DetailDialog extends DialogFragment implements OnClickListener{

	private final static String EXTRA_STORY = "Map";
	private final static String EXTRA_AUTHOR = "AUTHOR";
	private final static String EXTRA_REVIEW_BUTTON = "Reviews";
	
	private Story mStory;
	
	private final static int[] formats = {
				R.string.detail_author,
				R.string.detail_category,
				R.string.detail_rating,
				R.string.detail_language,
				R.string.detail_genre,
				R.string.detail_chapters,
				R.string.detail_words,
				R.string.detail_favorites,
				R.string.detail_follows,
				R.string.detail_reviews,
				R.string.detail_added,
				R.string.detail_last_read,
				R.string.detail_updated,
				R.string.detail_published,
				R.string.detail_story_id,
				R.string.detail_complete,
				R.string.detail_characters
	};
	
	public DetailDialog(){
	}

	private String asString(Date time)
	{
        return (time.getTime() <= 0L ? "" : DateFormat.getDateInstance().format(time));
	}
	
	@SuppressLint("InflateParams")
	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		super.onCreateDialog(savedInstanceState);
		
		mStory = getArguments().getParcelable(EXTRA_STORY);
		final boolean showAuthor = getArguments().getBoolean(EXTRA_AUTHOR);
		final boolean showReviewBtn = getArguments().getBoolean(EXTRA_REVIEW_BUTTON);

		int vPad = getResources().getDimensionPixelSize(R.dimen.activity_vertical_margin);
		int hPad = getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin);

		final ScrollView scrollView = new ScrollView(getActivity());
		final LinearLayout layout = new LinearLayout(getActivity());
		scrollView.addView(layout);
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setPadding(hPad, vPad, hPad, vPad);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(mStory.getName());
		builder.setView(scrollView);
		
		String[] stringValues = {
				mStory.getAuthor(),
				mStory.getCategory(),
				mStory.getRating(),
				mStory.getLanguage(),
				mStory.getGenre(),
				Integer.toString(mStory.getChapterLength()),
				mStory.getWordLength(),
				mStory.getFavorites(),
				mStory.getFollows(),
				Parser.withSuffix(mStory.getReviews()),
				asString(mStory.getAdded()),
				asString(mStory.getLastRead()),
				asString(mStory.getUpdated()),
				asString(mStory.getPublished()),
				Long.toString(mStory.getId()),
				mStory.isCompleted() ? getString(R.string.complete_true) : getString(R.string.complete_false),
				TextUtils.join(", ", mStory.getCharacters())
		};
		
		for (int i = 0; i < formats.length; i++) {
			if (stringValues[i].length() != 0 && !stringValues[i].equals("0")) {
				TextView label = new TextView(getActivity());
				label.setText(Html.fromHtml(getString(formats[i], stringValues[i])));
				layout.addView(label);
			}
		}

		if (showReviewBtn && !(mStory.getReviews() == 0)) {
			Button authorBtn = new Button(getActivity());
			authorBtn.setId(R.id.dialog_detail_review_btn);
			authorBtn.setText(R.string.detail_view_reviews);
			authorBtn.setOnClickListener(this);
			authorBtn.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			layout.addView(authorBtn);
		}

		if (showAuthor) {
			Button authorBtn = new Button(getActivity());
			authorBtn.setId(R.id.dialog_detail_author_btn);
			authorBtn.setText(R.string.menu_navigation_browse_by_author);
			authorBtn.setOnClickListener(this);
			authorBtn.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			layout.addView(authorBtn);
		}
		
		AlertDialog dialog= builder.create();
		dialog.setCanceledOnTouchOutside(true);
		return dialog;
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.dialog_detail_author_btn: {
				Uri.Builder builder = new Builder();
				builder.scheme(getString(R.string.fanfiction_scheme))
						.authority(getString(R.string.fanfiction_authority))
						.appendEncodedPath("u")
						.appendEncodedPath(mStory.getAuthorId() + "")
						.appendEncodedPath("");
				Intent i = new Intent(getActivity(), AuthorMenuActivity.class);
				i.setData(builder.build());
				startActivity(i);
				break;
			}
			case R.id.dialog_detail_review_btn: {
				Uri.Builder builder = new Builder();
				builder.scheme(getString(R.string.fanfiction_scheme))
						.authority(getString(R.string.fanfiction_authority))
						.appendEncodedPath("r")
						.appendEncodedPath(Long.toString(mStory.getId()))
						.appendEncodedPath("");
				Intent i = new Intent(getActivity(), ReviewMenuActivity.class);
				i.setData(builder.build());
				startActivity(i);
				break;
			}
		}
	}
	
	public static void show(FragmentActivity fragmentActivity, Story story) {

		boolean showAuthor = true;
		boolean showReview = true;

		// If the calling activity is an instance of LibraryMenuActivity, do not show the author
		// button since it is already present in the context menu. If the preceding activity
		// is an instance of AuthorMenuActivity, do not show the author button in order to avoid
		// the possibility of recursively entering thw AuthorMenuActivity.
		if (fragmentActivity instanceof AuthorMenuActivity || fragmentActivity instanceof LibraryMenuActivity) {
			showAuthor = false;
		}

		// If the calling activity is an instance of LibraryMenuActivity, do not show the author
		// button since it is already present in the context menu.
		if (fragmentActivity instanceof  LibraryMenuActivity){
			showReview = false;
		}

		DialogFragment dialog = new DetailDialog();
		Bundle bundle = new Bundle();
		bundle.putParcelable(EXTRA_STORY, story);
		bundle.putBoolean(EXTRA_AUTHOR, showAuthor);
		bundle.putBoolean(EXTRA_REVIEW_BUTTON, showReview);
		dialog.setArguments(bundle);
		dialog.show(fragmentActivity.getSupportFragmentManager(), null);
	}
	
}
