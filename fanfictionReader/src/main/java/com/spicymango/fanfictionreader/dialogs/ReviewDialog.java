package com.spicymango.fanfictionreader.dialogs;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;

import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.activity.LogInActivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AppCompatActivity;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

public class ReviewDialog extends DialogFragment {
	private static final String EXTRA_ID = "story id";
	private static final String EXTRA_PAGE = "page";
	
	private EditText review;
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		AlertDialog.Builder builder = new Builder(getActivity());
		
		Bundle in = getArguments();
		review = new EditText(getActivity());
		final int currentPage = in.getInt(EXTRA_PAGE);
		final long storyId = in.getLong(EXTRA_ID);
		
		review.setId(R.id.review_dialog_edit_text);
		review.setMinLines(3);
		review.setInputType(InputType.TYPE_CLASS_TEXT
				| InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
				| InputType.TYPE_TEXT_FLAG_MULTI_LINE);
		review.setHorizontallyScrolling(false);

		builder.setTitle(R.string.review);
		builder.setView(review);
		builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
			dismiss();
			Toast toast = Toast.makeText(getActivity(), R.string.dialog_cancelled, Toast.LENGTH_SHORT);
			toast.show();
		});

		builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
			PostReview post = new PostReview(getActivity(), currentPage, storyId, review.getText().toString());
			post.execute();

		});
		
		return builder.create();
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		review.requestFocus();
	}
	
	public static void review(AppCompatActivity context, long storyId, int currentPage){
		DialogFragment dialog = new ReviewDialog();
		
		Bundle bundle = new Bundle();
		bundle.putLong(EXTRA_ID, storyId);
		bundle.putInt(EXTRA_PAGE, currentPage);
		
		dialog.setArguments(bundle);
		dialog.show(context.getSupportFragmentManager(), ReviewDialog.class.getName());
	}

	private final static class PostReview extends AsyncTask<Void, Void, Boolean>{
		private final int mCurrentPage;
		private final long mStoryId;
		private final Context mContext;
		private final String mReview, scheme, authority;
		private Map<String, String> cookies;
		private String mErrorCode;
		
		
		public PostReview(Context context, int currentPage, long storyId, String review) {
			mContext = context;
			mCurrentPage = currentPage;
			mStoryId = storyId;
			mReview = review;
			scheme = mContext.getString(R.string.fanfiction_scheme);
			authority = mContext.getString(R.string.fanfiction_authority);
			cookies = LogInActivity.getCookies(mContext);
		}

		@Override
		protected Boolean doInBackground(Void... params) {
					
			Uri.Builder story = new Uri.Builder();
			story.scheme(scheme);
			story.authority(authority);
			story.appendPath("s");
			story.appendPath(Long.toString(mStoryId));
			story.appendPath(Integer.toString(mCurrentPage));
			story.appendPath("");

			try {
				Response res = Jsoup.connect(story.build().toString())
						.cookies(cookies).timeout(10000).method(Method.GET)
						.execute();
				String html = res.body();

				Pattern pattern = Pattern.compile("(?:storytextid=)(\\d++)");
				Matcher matcher = pattern.matcher(html);
				if (!matcher.find()) {
					return false;
				}

				long textId = Long.parseLong(matcher.group(1));

				Uri.Builder uri = new Uri.Builder();
				uri.scheme(scheme);
				uri.authority(authority);
				uri.appendPath("api");
				uri.appendPath("ajax_review.php");
				
				HashMap<String, String> data = new HashMap<>();
				data.put("storyid", Long.toString(mStoryId));
				data.put("storytextid", Long.toString(textId));
				data.put("chapter", Integer.toString(mCurrentPage));
				data.put("review", mReview);
				data.put("authoralert", "0");
				data.put("storyalert", "0");
				data.put("favstory", "0");
				data.put("favauthor", "0");

				res = Jsoup.connect(uri.build().toString()).cookies(cookies)
						.timeout(10000).data(data).method(Method.POST).execute();
				
				
				if (res.body().contains("\"error\":true")) {
					Pattern errorPattern = Pattern.compile("\"error_msg\":\"([^\"]++)");
					Matcher errorMatcher = errorPattern.matcher(res.body());
					if(errorMatcher.find()){
						mErrorCode = errorMatcher.group(1);
						return false;
					}
				}
				
				return true;
			} catch (IOException e) {
				return false;
			}
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if (result) {
				Toast toast = Toast.makeText(mContext,
						R.string.dialog_review_posted, Toast.LENGTH_SHORT);
				toast.show();
			} else if(mErrorCode == null){
				Toast toast = Toast.makeText(mContext,
						R.string.error_connection, Toast.LENGTH_SHORT);
				toast.show();
			} else{
				Toast toast = Toast.makeText(mContext, mErrorCode,
						Toast.LENGTH_LONG);
				toast.show();
			}
		}
	}
}

