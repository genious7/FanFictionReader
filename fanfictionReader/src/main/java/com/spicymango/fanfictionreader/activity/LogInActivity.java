package com.spicymango.fanfictionreader.activity;

import java.util.HashMap;
import java.util.Map;

import com.spicymango.fanfictionreader.Settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.MenuItem;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class LogInActivity extends AppCompatActivity {
	private static final String PREF_COOKIE = "Cookie";

	/**
	 * Gets the cookies that indicate whether the user is logged in or not
	 * @param context The current context
	 * @return A map containing the valid cookies.
	 */
	public static Map<String, String> getCookies(Context context) {
		
		//Get the cookies, which are stored as a single string
		SharedPreferences pref = context.getSharedPreferences(PREF_COOKIE,
				MODE_PRIVATE);
		String cookies = pref.getString(PREF_COOKIE, null);

		//If there are no cookies available, return null
		if (cookies == null) {
			return null;
		}

		//Split the cookie string into a HashMap
		String[] tmp = cookies.split(";");
		Map<String, String> cookieMap = new HashMap<>();
		for (String string : tmp) {
			String[] cookie = string.split("=");
			cookieMap.put(cookie[0], cookie[1]);
		}

		return cookieMap;
	}
	
	public static boolean isLoggedIn(Context context) {

		// Get the cookies, which are stored as a single string
		SharedPreferences pref = context.getSharedPreferences(PREF_COOKIE,
				MODE_PRIVATE);
		String cookies = pref.getString(PREF_COOKIE, null);

		// If there are no cookies available, return null
		return cookies != null;
	}

	/**
	 * Logs out the user
	 * @param context The current context
	 */
	public static void logOut(Context context) {
		SharedPreferences pref = context.getSharedPreferences(PREF_COOKIE,
				MODE_PRIVATE);
		SharedPreferences.Editor editor = pref.edit();
		editor.remove(PREF_COOKIE);
		editor.commit();
		
		//Delete the cookies so that the user doesn't instantly log back in
		CookieSyncManager.createInstance(context);
		CookieManager manager = CookieManager.getInstance();
		manager.removeAllCookie();
		CookieSyncManager.getInstance().sync();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Saves the cookies as a preference
	 * @param cookies The cookies formated as a single string
	 */
	private void setCookies(String cookies) {
		SharedPreferences pref = getSharedPreferences(PREF_COOKIE, MODE_PRIVATE);
		SharedPreferences.Editor editor = pref.edit();
		editor.putString(PREF_COOKIE, cookies);
		editor.commit();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Settings.setOrientationAndTheme(this);
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		WebView view = new WebView(this);
		view.getSettings().setUserAgentString("Mozilla/5.0");
		view.getSettings().setJavaScriptEnabled(true);
		view.setWebViewClient(new WebClient());
		setContentView(view);
		view.loadUrl("https://www.fanfiction.net/login.php");

	}

	private class WebClient extends WebViewClient {

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			
			if (url.startsWith("https://www.fanfiction.net/account/settings.php")) {
				// If successfully logged in, stop
				String cookies = CookieManager.getInstance().getCookie(
						"https://www.fanfiction.net/");
				setCookies(cookies);
				LogInActivity.this.setResult(RESULT_OK);
				finish();
				return true;
			} else {
				return false;
			}
		}
	}
}
