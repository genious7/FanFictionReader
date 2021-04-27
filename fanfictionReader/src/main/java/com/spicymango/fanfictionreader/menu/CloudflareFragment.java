package com.spicymango.fanfictionreader.menu;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.net.CookieHandler;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

/**
 * A fragment that opens a web view to solve the CloudFlare captcha.
 * Results are saved by updating the cookies in the {@link com.spicymango.fanfictionreader.util.AndroidCookieStore}
 */
public class CloudflareFragment extends Fragment {
	public final static String EXTRA_URI = "uri";

	private WebView mWebView;
	private final Object mutex = new Object();
	private boolean mHasLoaded;


	@SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater,
							 @Nullable  ViewGroup container,
							 @Nullable Bundle savedInstanceState) {

		assert getArguments() != null;
		URI uri;
		try {
			uri = new URI(getArguments().getParcelable(EXTRA_URI).toString());
		} catch (URISyntaxException e) {
			uri = null;
			Log.e(this.getClass().getSimpleName(), e.getMessage());
		}
		assert uri != null;


		// Set the webView cookies to match the http cookies
		CookieSyncManager.createInstance(requireContext());
		final CookieManager cookieManager = CookieManager.getInstance();
		cookieManager.setAcceptCookie(true);
		final CookieStore cookieStore = ((java.net.CookieManager) CookieHandler.getDefault()).getCookieStore();
		final List<HttpCookie> cookieList = cookieStore.get(uri);
		for (HttpCookie cookie : cookieList){
			URI baseUri = uri;

			// If the HttpCookie has a domain attribute, use that over the provided uri.
			if (cookie.getDomain() != null){
				// Remove the starting dot character of the domain, if exists (e.g: .domain.com -> domain.com)
				String domain = cookie.getDomain();
				if (domain.charAt(0) == '.') {
					domain = domain.substring(1);
				}

				// Create the new URI
				try{
					baseUri = new URI(uri.getScheme() == null ? "http" : uri.getScheme(),
								  domain,
								  cookie.getPath() == null ? "/" : cookie.getPath(),
								  null);
				} catch (URISyntaxException e) {
					Log.w(this.getClass().getSimpleName(), e);
				}
			}

			String cookieHeader = cookie.toString() + "; domain=" + cookie.getDomain() +
					"; path=" + cookie.getPath();
			cookieManager.setCookie(baseUri.toString(), cookieHeader);
		}


		mWebView = new WebView(requireContext());
		mWebView.getSettings().setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36");
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.setWebViewClient(new CustomWebView());
		mWebView.addJavascriptInterface(new JavascriptListener(), "HTMLOUT");
		mWebView.loadUrl(uri.toString());

		return mWebView;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		// Stop loading any page if the fragment is destroyed
		mWebView.stopLoading();
	}

	/**
	 * Closes this fragment whilst returning the result to the calling fragment.
	 * @param result The string that should be passed to the main fragment.
	 */
	private void closeFragment(String result){

		if (!isAdded())	return; // If the activity has been closed, then disregard the results.

		final Bundle results = new Bundle();
		results.putString("DATA", result);

		final FragmentManager manager = getParentFragmentManager();
		manager.setFragmentResult("DATA_CLOUDFLARE", results);
		manager.beginTransaction().remove(CloudflareFragment.this).commit();
	}

	/**
	 * Waits 5 seconds (just in case the current error is a Cloudflare wait page), then closes the
	 * fragment, returning an error.
	 */
	private void waitThenCloseFragment(){
		if (!isAdded())	return; // If the activity has been closed, then disregard the results.

		mHasLoaded = false;

		final Runnable runnable = () -> {
			try {
				synchronized (mutex){
					mutex.wait(5*1000); // Wait 5 seconds before timing out
				}
			} catch (InterruptedException ignored) {}

			// If the page hasn't loaded by then, close the fragment whilst throwing an error.
			if (!mHasLoaded){
				// Close the fragment from the main thread.
				new Handler(Looper.getMainLooper()).post(()-> closeFragment("404"));
			}
		};

		new Thread(runnable).start();
	}

	private class JavascriptListener{
		@android.webkit.JavascriptInterface
		public void processHTML(String html){
			closeFragment(html);
		}
	}

	private class CustomWebView extends WebViewClient{

		@Override
		public void onReceivedError(WebView view, WebResourceRequest request,
									WebResourceError error) {
			super.onReceivedError(view, request, error);
			closeFragment("404");
		}

		@Override
		public void onReceivedHttpError(WebView view, WebResourceRequest request,
										WebResourceResponse errorResponse) {
			super.onReceivedHttpError(view, request, errorResponse);
			waitThenCloseFragment();
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			// Stop the HttpErrorTimer if it is running.
			mHasLoaded = true;
			synchronized (mutex) {
				mutex.notify();
			}

			// Pass the cookies from the webView to the cookie storage
			final CookieManager manager = CookieManager.getInstance();
			final String httpCookieHeader = manager.getCookie(url);

			if (httpCookieHeader != null){
				try {
					final URI uri = new URI("https://fanfiction.net/");
					final CookieStore cookieStore = ((java.net.CookieManager) CookieHandler.getDefault()).getCookieStore();

					for (String cookieString : httpCookieHeader.split(";")){
						final String[] splitCookie = cookieString.split("=");
						HttpCookie cookie = new HttpCookie(splitCookie[0],splitCookie[1]);
						cookieStore.add(uri, cookie);
					}

				} catch (URISyntaxException e) {
					Log.d(getClass().getSimpleName(), "Failed to parse URI =" + url, e);
				}
			}

			// Retrieve the html code.
			view.loadUrl("javascript:window.HTMLOUT.processHTML('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');");
			super.onPageFinished(view, url);
		}
	}
}
