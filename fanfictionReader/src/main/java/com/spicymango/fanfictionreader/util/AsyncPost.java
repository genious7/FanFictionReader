package com.spicymango.fanfictionreader.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jsoup.Connection.Method;
import org.jsoup.Jsoup;

import com.spicymango.fanfictionreader.R;
import com.spicymango.fanfictionreader.activity.LogInActivity;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.Toast;

public class AsyncPost extends AsyncTask<Void, Void, Boolean> {
	
	 
	
	private static final int negMsg = R.string.error_connection;
	private final Context context;
	private final Map<String, String> cookies;
	private final Map<String, String> data;
	private final int posMsg;
	private final Uri uri;
	private final Method method;

	/**
	 * Creates a new AsyncPost
	 * @param context The current context
	 * @param positiveMsg The message to display on success
	 */
	public AsyncPost(Context context, int positiveMsg, Uri uri, Method method) {
		this(context, positiveMsg, null, uri, method);
	}
	
	public AsyncPost(Context context, int positiveMsg, Map<String, String> data, Uri uri, Method method){
		this.context = context;
		this.uri = uri;
		cookies = LogInActivity.getCookies(context);
		posMsg = positiveMsg;
		this.method = method;
		if (data == null) {
			this.data = new HashMap<String, String>();
		}else{
			this.data = data;
		}		
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		try {
			Jsoup.connect(uri.toString()).cookies(cookies).timeout(10000).data(data).method(method).execute();
		} catch (IOException e) {
			return false;
		}
		
		return true;
	}

	@Override
	protected void onPostExecute(Boolean result) {
		super.onPostExecute(result);
		if (result) {
			Toast toast = Toast.makeText(context, posMsg, Toast.LENGTH_SHORT);
			toast.show();
		} else {
			Toast toast = Toast.makeText(context, negMsg, Toast.LENGTH_SHORT);
			toast.show();
		}

	}

}
