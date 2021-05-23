package com.spicymango.fanfictionreader.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A {@link CookieStore} that will persistently store cookies between app restarts.
 */
public class AndroidCookieStore implements CookieStore {
	/** The name of the {@link SharedPreferences} for the cookie store*/
	private static final String SP_COOKIE_STORE = "cookieStore";

	/** A delimiter in the shared preference file*/
	private static final String SP_KEY_DELIMITER = "|"; // Unusual char in URL
	private static final String SP_KEY_DELIMITER_REGEX = "\\|"; // Unusual char in URL

	/** The memory storage of cookies**/
	private final Map<URI, List<HttpCookie>> mCookieMap;

	private final SharedPreferences mSharedPreferences;

	public AndroidCookieStore(final Context context){
		mCookieMap = new HashMap<>();
		mSharedPreferences = context.getSharedPreferences(SP_COOKIE_STORE,Context.MODE_PRIVATE);

		// Try to load the cookieMap from the sharedPreference
		final Map<String,?> map = mSharedPreferences.getAll();
		for (Map.Entry<String, ?> mapEntry : map.entrySet()){
			final String[] uriArray = mapEntry.getKey().split(SP_KEY_DELIMITER_REGEX, 2);

			try {
				final URI uri = new URI(uriArray[0]);
				final String encodedCookie = (String) mapEntry.getValue();
				final HttpCookie cookie = new SerializableHttpCookie().decode(encodedCookie);

				List<HttpCookie> currentCookies = mCookieMap.get(uri);
				if (currentCookies == null){
					currentCookies = new ArrayList<>();
					mCookieMap.put(uri, currentCookies);
				}
				currentCookies.add(cookie);

			}catch (URISyntaxException e){
				Log.w(this.getClass().getSimpleName(), e);
			}
		}

	}

	/**
	 * Saves the map to the {@link SharedPreferences}
	 */
	private void flush(){
		final SharedPreferences.Editor editor = mSharedPreferences.edit();

		for (Map.Entry<URI, List<HttpCookie>> cookieList : mCookieMap.entrySet()){
			for (HttpCookie cookie : cookieList.getValue()){
				editor.putString(cookieList.getKey().toString() + SP_KEY_DELIMITER + cookie.getName(),
								 new SerializableHttpCookie().encode(cookie));
			}
		}
		editor.apply();
	}

	/**
	 * @see CookieStore#add(URI, HttpCookie) 
	 */
	@Override
	public void add(URI uri, HttpCookie httpCookie) {
		// If the HttpCookie has a domain attribute, use that over the provided uri.
		if (httpCookie.getDomain() != null){
			// Remove the starting dot character of the domain, if exists (e.g: .domain.com -> domain.com)
			String domain = httpCookie.getDomain();
			if (domain.charAt(0) == '.') {
				domain = domain.substring(1);
			}

			// Create the new URI
			try{
				uri = new URI(uri.getScheme() == null ? "http" : uri.getScheme(),
							  domain,
							  httpCookie.getPath() == null ? "/" : httpCookie.getPath(),
							  null);
			} catch (URISyntaxException e) {
				Log.w(this.getClass().getSimpleName(), e);
			}
		}

		List<HttpCookie> cookieList = mCookieMap.get(uri);
		if (cookieList == null){
			cookieList = new ArrayList<>();
			mCookieMap.put(uri, cookieList);
		} else{
			// If a cookie has the same name, path, and domain, overwrite it with the new one.
			final ListIterator<HttpCookie> iterator = cookieList.listIterator();
			while (iterator.hasNext()){
				final HttpCookie existingCookie = iterator.next();
				if (existingCookie != null && existingCookie.equals(httpCookie)){
					iterator.set(httpCookie);
					return;
				}
			}
		}

		cookieList.add(httpCookie);
		flush();
	}

	/**
	 * @see	CookieStore#get(URI) 
	 */
	@Override
	public List<HttpCookie> get(URI uri) {
		final ArrayList<HttpCookie> returnList = new ArrayList<>();

		for (URI entryUri : mCookieMap.keySet()){
			// Check if the URI is valid
			if (checkDomainsMatch(entryUri.getHost(), uri.getHost()) &&
				checkPathsMatch(entryUri.getPath(), uri.getPath())){
				returnList.addAll(Objects.requireNonNull(mCookieMap.get(entryUri)));
			}
		}

		// Remove all expired cookies
		final Iterator<HttpCookie> iterator = returnList.iterator();
		while (iterator.hasNext()){
			final HttpCookie cookie = iterator.next();
			if (cookie == null || cookie.hasExpired()) // Remove null or expired cookies
				iterator.remove();
		}

		return returnList;
	}

	/**
	 * @see CookieStore#getCookies()
	 */
	@Override
	public List<HttpCookie> getCookies() {
		final ArrayList<HttpCookie> returnValue = new ArrayList<>();
		for (List<HttpCookie> cookieList : mCookieMap.values()){

			// Use an iterator instead of a foreach to avoid ConcurrentModificationException
			final Iterator<HttpCookie> iterator = cookieList.iterator();
			while (iterator.hasNext()){
				final HttpCookie cookie = iterator.next();
				if (cookie == null || cookie.hasExpired()) // Remove null or expired cookies
					iterator.remove();
				else
					returnValue.add(cookie);
			}
		}

		return returnValue;
	}

	/**
	 * @see CookieStore#getURIs()
	 */
	@Override
	public List<URI> getURIs() {
		final Set<URI> keys = mCookieMap.keySet();
		return new ArrayList<>(keys);
	}

	/**
	 * @see java.net.CookieStore#remove(URI, HttpCookie)
	 */
	@Override
	public boolean remove(URI uri, HttpCookie httpCookie) {
		// If the HttpCookie has a domain attribute, use that over the provided uri.
		if (httpCookie.getDomain() != null){
			// Remove the starting dot character of the domain, if exists (e.g: .domain.com -> domain.com)
			String domain = httpCookie.getDomain();
			if (domain.charAt(0) == '.') {
				domain = domain.substring(1);
			}

			// Create the new URI
			try{
				uri = new URI(uri.getScheme() == null ? "http" : uri.getScheme(),
							  domain,
							  httpCookie.getPath() == null ? "/" : httpCookie.getPath(),
							  null);
			} catch (URISyntaxException e) {
				Log.w(this.getClass().getSimpleName(), e);
			}
		}

		final List<HttpCookie> cookies = mCookieMap.get(uri);
		boolean returnValue = false;
		if (cookies == null){
			return false;
		} else{
			returnValue = cookies.remove(httpCookie);
			flush();
			return returnValue;
		}
	}

	/**
	 * @see CookieStore#removeAll()
	 */
	@Override
	public boolean removeAll() {
		boolean returnValue = !mCookieMap.isEmpty();
		mCookieMap.clear();
		flush();
		return returnValue;
	}

	   /** http://tools.ietf.org/html/rfc6265#section-5.1.3
    A string domain-matches a given domain string if at least one of the
    following conditions hold:
    o  The domain string and the string are identical.  (Note that both
    the domain string and the string will have been canonicalized to
    lower case at this point.)
    o  All of the following conditions hold:
        *  The domain string is a suffix of the string.
        *  The last character of the string that is not included in the
           domain string is a %x2E (".") character.
        *  The string is a host name (i.e., not an IP address). */

	private boolean checkDomainsMatch(String cookieHost, String requestHost) {
		return requestHost.equals(cookieHost) || requestHost.endsWith("." + cookieHost);
	}

    /**  http://tools.ietf.org/html/rfc6265#section-5.1.4
        A request-path path-matches a given cookie-path if at least one of
        the following conditions holds:
        o  The cookie-path and the request-path are identical.
        o  The cookie-path is a prefix of the request-path, and the last
        character of the cookie-path is %x2F ("/").
        o  The cookie-path is a prefix of the request-path, and the first
        character of the request-path that is not included in the cookie-
        path is a %x2F ("/") character. */

	private boolean checkPathsMatch(String cookiePath, String requestPath) {
		return requestPath.equals(cookiePath) ||
				(requestPath.startsWith(cookiePath) && cookiePath.charAt(cookiePath.length() - 1) == '/') ||
				(requestPath.startsWith(cookiePath) && requestPath.substring(cookiePath.length()).charAt(0) == '/');
	}

	public static class SerializableHttpCookie implements Serializable {
		private static final String TAG = SerializableHttpCookie.class
				.getSimpleName();

		private static final long serialVersionUID = 6374381323722046732L;

		private transient HttpCookie cookie;

		// Workaround httpOnly: The httpOnly attribute is not accessible so when we
		// serialize and deserialize the cookie it does not preserve the same value. We
		// need to access it using reflection
		private Field fieldHttpOnly;

		public SerializableHttpCookie() {
		}

		public String encode(HttpCookie cookie) {
			this.cookie = cookie;
			String returnString;
			try {
				final ByteArrayOutputStream os = new ByteArrayOutputStream();
				final ObjectOutputStream outputStream = new ObjectOutputStream(os);
				outputStream.writeObject(this);
				returnString = new String(os.toByteArray(), "ISO-8859-1");
			} catch (IOException e) {
				Log.d(TAG, "IOException in encodeCookie", e);
				return null;
			}

			return returnString;
		}

		public HttpCookie decode(String encodedCookie) {

			HttpCookie cookie = null;
			try {
				byte[] bytes = encodedCookie.getBytes("ISO-8859-1");
				final InputStream byteArrayInputStream = new ByteArrayInputStream(
						bytes);
				final ObjectInputStream objectInputStream = new ObjectInputStream(
						byteArrayInputStream);
				cookie = ((SerializableHttpCookie) objectInputStream.readObject()).cookie;
			} catch (IOException e) {
				Log.d(TAG, "IOException in decodeCookie", e);
			} catch (ClassNotFoundException e) {
				Log.d(TAG, "ClassNotFoundException in decodeCookie", e);
			}
			return cookie;
		}

		// Workaround httpOnly (getter)
		private boolean getHttpOnly() {
			try {
				initFieldHttpOnly();
				return (boolean) fieldHttpOnly.get(cookie);
			} catch (Exception e) {
				// NoSuchFieldException || IllegalAccessException ||
				// IllegalArgumentException
				Log.w(TAG, e);
			}
			return false;
		}

		// Workaround httpOnly (setter)
		private void setHttpOnly(boolean httpOnly) {
			try {
				initFieldHttpOnly();
				fieldHttpOnly.set(cookie, httpOnly);
			} catch (Exception e) {
				// NoSuchFieldException || IllegalAccessException ||
				// IllegalArgumentException
				Log.w(TAG, e);
			}
		}

		private void initFieldHttpOnly() throws NoSuchFieldException {
			fieldHttpOnly = cookie.getClass().getDeclaredField("httpOnly");
			fieldHttpOnly.setAccessible(true);
		}

		private void writeObject(ObjectOutputStream out) throws IOException {
			out.writeObject(cookie.getName());
			out.writeObject(cookie.getValue());
			out.writeObject(cookie.getComment());
			out.writeObject(cookie.getCommentURL());
			out.writeObject(cookie.getDomain());
			out.writeLong(cookie.getMaxAge());
			out.writeObject(cookie.getPath());
			out.writeObject(cookie.getPortlist());
			out.writeInt(cookie.getVersion());
			out.writeBoolean(cookie.getSecure());
			out.writeBoolean(cookie.getDiscard());
			out.writeBoolean(getHttpOnly());
		}

		private void readObject(ObjectInputStream in) throws IOException,
				ClassNotFoundException {
			String name = (String) in.readObject();
			String value = (String) in.readObject();
			cookie = new HttpCookie(name, value);
			cookie.setComment((String) in.readObject());
			cookie.setCommentURL((String) in.readObject());
			cookie.setDomain((String) in.readObject());
			cookie.setMaxAge(in.readLong());
			cookie.setPath((String) in.readObject());
			cookie.setPortlist((String) in.readObject());
			cookie.setVersion(in.readInt());
			cookie.setSecure(in.readBoolean());
			cookie.setDiscard(in.readBoolean());
			setHttpOnly(in.readBoolean());
		}
	}
}
