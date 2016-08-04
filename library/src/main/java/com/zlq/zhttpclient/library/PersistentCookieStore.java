package com.zlq.zhttpclient.library;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by zhanglq on 16/8/1.
 */
public class PersistentCookieStore implements CookieStore {

	private static final String LOG_TAG = "PersistentCookieStore";
	private static final String COOKIE_PREFS = "CookiePrefsFile";
	private static final String COOKIE_NAME_STORE = "names";
	private static final String COOKIE_NAME_PREFIX = "cookie_";
	private boolean omitNonPersistentCookies = false;

	private final ConcurrentHashMap<String, HttpCookie> cookies;
	private final SharedPreferences cookiePrefs;

	public PersistentCookieStore(Context context){
		cookiePrefs = context.getSharedPreferences(COOKIE_PREFS, 0);
		cookies = new ConcurrentHashMap<String, HttpCookie>();

		// Load any previously stored cookies into the store
		String storedCookieNames = cookiePrefs.getString(COOKIE_NAME_STORE, null);
		if (storedCookieNames != null) {
			String[] cookieNames = TextUtils.split(storedCookieNames, ",");
			for (String name : cookieNames) {
				String encodedCookie = cookiePrefs.getString(COOKIE_NAME_PREFIX + name, null);
				if (encodedCookie != null) {
					HttpCookie decodedCookie = decodeCookie(encodedCookie);
					if (decodedCookie != null) {
						cookies.put(name, decodedCookie);
					}
				}
			}

			// Clear out expired cookies
			clearExpired();
		}
	}

	@Override
	public void add(URI uri, HttpCookie cookie) {
		if (omitNonPersistentCookies) return;
		String name = cookie.getName() + cookie.getDomain() + (uri == null ? "" : uri.toString());
		// Save cookie into local store, or remove if expired
		if (!cookie.hasExpired()) {
			cookies.put(name, cookie);
		} else {
			cookies.remove(name);
		}
		// Save cookie into persistent store
		SharedPreferences.Editor prefsWriter = cookiePrefs.edit();
		prefsWriter.putString(COOKIE_NAME_STORE, TextUtils.join(",", cookies.keySet()));
		prefsWriter.putString(COOKIE_NAME_PREFIX + name, encodeCookie(new SerializableCookie(cookie)));
		prefsWriter.commit();
	}

	@Override
	public List<HttpCookie> get(URI uri) {
		ArrayList<HttpCookie> result = new ArrayList<HttpCookie>();
		for (ConcurrentHashMap.Entry<String, HttpCookie> entry : cookies.entrySet()) {
			String cookieName = entry.getKey();
			HttpCookie cookie = entry.getValue();
			String beforName = cookie.getName() + cookie.getDomain();
			if (uri == null && cookieName.equals(beforName)){
				result.add(cookie);
			}else if(uri != null && !cookieName.equals(beforName) && cookieName.length() > beforName.length()){
				if (uri.equals(cookieName.substring(beforName.length(), cookieName.length()))){
					result.add(cookie);
				}
			}
		}
		//Log.i(LOG_TAG, "uri = " + uri.toString() + " cookies = " + TextUtils.join(";", result));
		return result;
	}

	@Override
	public List<HttpCookie> getCookies() {
		ArrayList<HttpCookie> cookiesArr = new ArrayList<HttpCookie>(cookies.values());
		//Log.i(LOG_TAG, "cookies = " + TextUtils.join(";", cookiesArr));
		return cookiesArr;
	}

	@Override
	public List<URI> getURIs() {
		ArrayList<URI> uris = new ArrayList<URI>();
		for (ConcurrentHashMap.Entry<String, HttpCookie> entry : cookies.entrySet()) {
			String cookieName = entry.getKey();
			HttpCookie cookie = entry.getValue();
			String beforName = cookie.getName() + cookie.getDomain();
			if(!cookieName.equals(beforName) && cookieName.length() > beforName.length()){
				String uriStr = cookieName.substring(beforName.length(), cookieName.length());
				if (uriStr != null && uriStr.length() > 0){
					uris.add(URI.create(uriStr));
				}
			}
		}
		//Log.i(LOG_TAG, "uris = " + TextUtils.join(";", uris));
		return uris;
	}

	@Override
	public boolean remove(URI uri, HttpCookie cookie) {
		SharedPreferences.Editor prefsWriter = cookiePrefs.edit();
		String name = cookie.getName() + cookie.getDomain() + (uri == null ? "" : uri.toString());
		prefsWriter.remove(COOKIE_NAME_PREFIX + name);
		prefsWriter.remove(COOKIE_NAME_STORE);
		prefsWriter.commit();

		cookies.remove(name);
		cookiePrefs.edit().putString(COOKIE_NAME_STORE, TextUtils.join(",", cookies.keySet())).commit();
		return true;
	}

	@Override
	public boolean removeAll() {
		SharedPreferences.Editor prefsWriter = cookiePrefs.edit();
		for (String name : cookies.keySet()) {
			prefsWriter.remove(COOKIE_NAME_PREFIX + name);
		}
		prefsWriter.remove(COOKIE_NAME_STORE);
		prefsWriter.commit();

		// Clear cookies from local store
		cookies.clear();
		return true;
	}

	/**
	 * Serializes Cookie object into String
	 *
	 * @param cookie cookie to be encoded, can be null
	 * @return cookie encoded as String
	 */
	protected String encodeCookie(SerializableCookie cookie) {
		if (cookie == null)
			return null;
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try {
			ObjectOutputStream outputStream = new ObjectOutputStream(os);
			outputStream.writeObject(cookie);
		} catch (IOException e) {
			Log.d(LOG_TAG, "IOException in encodeCookie", e);
			return null;
		}

		return byteArrayToHexString(os.toByteArray());
	}

	/**
	 * Returns cookie decoded from cookie string
	 *
	 * @param cookieString string of cookie as returned from http request
	 * @return decoded cookie or null if exception occured
	 */
	protected HttpCookie decodeCookie(String cookieString) {
		byte[] bytes = hexStringToByteArray(cookieString);
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
		HttpCookie cookie = null;
		try {
			ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
			cookie = ((SerializableCookie) objectInputStream.readObject()).getCookie();
		} catch (IOException e) {
			Log.d(LOG_TAG, "IOException in decodeCookie", e);
		} catch (ClassNotFoundException e) {
			Log.d(LOG_TAG, "ClassNotFoundException in decodeCookie", e);
		}

		return cookie;
	}

	/**
	 * Using some super basic byte array &lt;-&gt; hex conversions so we don't have to rely on any
	 * large Base64 libraries. Can be overridden if you like!
	 *
	 * @param bytes byte array to be converted
	 * @return string containing hex values
	 */
	protected String byteArrayToHexString(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (byte element : bytes) {
			int v = element & 0xff;
			if (v < 16) {
				sb.append('0');
			}
			sb.append(Integer.toHexString(v));
		}
		return sb.toString().toUpperCase(Locale.US);
	}

	/**
	 * Converts hex values from strings to byte arra
	 *
	 * @param hexString string of hex-encoded values
	 * @return decoded byte array
	 */
	protected byte[] hexStringToByteArray(String hexString) {
		int len = hexString.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4) + Character.digit(hexString.charAt(i + 1), 16));
		}
		return data;
	}

	public boolean clearExpired() {
		boolean clearedAny = false;
		SharedPreferences.Editor prefsWriter = cookiePrefs.edit();

		for (ConcurrentHashMap.Entry<String, HttpCookie> entry : cookies.entrySet()) {
			String name = entry.getKey();
			HttpCookie cookie = entry.getValue();
			if (cookie.hasExpired()) {
				// Clear cookies from local store
				cookies.remove(name);

				// Clear cookies from persistent store
				prefsWriter.remove(COOKIE_NAME_PREFIX + name);

				// We've cleared at least one
				clearedAny = true;
			}
		}

		// Update names in persistent store
		if (clearedAny) {
			prefsWriter.putString(COOKIE_NAME_STORE, TextUtils.join(",", cookies.keySet()));
		}
		prefsWriter.commit();

		return clearedAny;
	}

	public void setOmitNonPersistentCookies(boolean omitNonPersistentCookies) {
		this.omitNonPersistentCookies = omitNonPersistentCookies;
	}
}
