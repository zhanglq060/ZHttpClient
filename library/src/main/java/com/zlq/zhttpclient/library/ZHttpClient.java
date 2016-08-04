package com.zlq.zhttpclient.library;

import android.content.Context;
import android.text.TextUtils;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;

/**
 * Created by zhanglq on 15/5/30.
 */
public class ZHttpClient {

	public static final String LOG_TAG = "ZHttpClient";

	public static final String HEADER_CONTENT_ENCODING = "Content-Encoding";
	public static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
	public static final String HEADER_REQUEST_COOKIE = "Cookie";
	public static final String ENCODING_GZIP = "gzip";
	public static final String HEADER_USER_AGENT = "User-Agent";

	public static final String VERSION = "1.1.0";
	public static final int DEFAULT_HTTP_PORT = 80;
	public static final int DEFAULT_HTTPS_PORT = 443;
	public static final int DEFAULT_MAX_CONNECTIONS = 10;
	public static final int DEFAULT_SOCKET_TIMEOUT = 20 * 1000;

	private int maxConnections = DEFAULT_MAX_CONNECTIONS;
	private int timeout = DEFAULT_SOCKET_TIMEOUT;

	private ExecutorService threadPool;
	private final Map<Context, List<RequestHandle>> requestMap;
	private final Map<String, String> clientHeaderMap;
	private boolean isUrlEncodingEnabled = true;
	private SSLSocketFactory mSSLSocketFactory;
	private CookieManager mCookieManager;
	private int httpPort;
	private int httpsPort;
	private String encodingCharset;

	public ZHttpClient(){
		this(DEFAULT_HTTP_PORT);
	}

	public ZHttpClient(int httpPort){
		this(httpPort, DEFAULT_HTTPS_PORT);
	}

	public ZHttpClient(int httpPort, int httpsPort){
		this.httpPort = httpPort;
		this.httpsPort = httpsPort;
		requestMap = new HashMap<Context, List<RequestHandle>>();
		clientHeaderMap = new HashMap<String, String>();

		setHeader(HEADER_USER_AGENT, String.format("ZHttpClient/%s (https://github.com/zhanglq060/ZHttpClient)", VERSION));
		//default content_encoding is gzip
		setHeader(HEADER_CONTENT_ENCODING, ENCODING_GZIP);
		setHeader(HEADER_ACCEPT_ENCODING, ENCODING_GZIP);

		threadPool = getDefaultThreadPool();
	}

	/**
	 * 通过URL 初始化 HttpURLConnection
	 * @param url 请求的URL
	 * @return HttpURLConnection
	 * @throws IOException
	 */
	private HttpURLConnection initUrlConnection(URL url) throws IOException {
		if(url == null || url.toString().length() == 0){
			throw new RuntimeException("initConnection url is empty");
		}
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		if (mCookieManager != null){
			setHeader(HEADER_REQUEST_COOKIE, TextUtils.join(";", mCookieManager.getCookieStore().get(URI.create(url.getHost()))));
		}

		Iterator iterator = clientHeaderMap.entrySet().iterator();
		while (iterator.hasNext()){
			Map.Entry<String,String> entry = (Map.Entry<String, String>) iterator.next();
			connection.setRequestProperty(entry.getKey(), entry.getValue());
		}

		if(connection instanceof HttpsURLConnection && mSSLSocketFactory != null){
			((HttpsURLConnection)connection).setSSLSocketFactory(mSSLSocketFactory);
			((HttpsURLConnection)connection).setHostnameVerifier(new HostnameVerifier() {
				@Override
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}
			});
		}
		connection.setConnectTimeout(timeout);
		connection.setReadTimeout(timeout);
		connection.setUseCaches(false);
		return connection;
	}

	/**
	 * Get the default threading pool to be used for this HTTP client.
	 *
	 * @return The default threading pool to be used
	 */
	protected ExecutorService getDefaultThreadPool() {
		return Executors.newCachedThreadPool();
	}

	public void setHeader(String key, String value){
		clientHeaderMap.put(key, value);
	}

	public RequestHandle get(String url, RequestParams params, ResponseHandlerInterface listener){
		return get(null, url, params, listener);
	}

	public RequestHandle get(Context context, String url, RequestParams params, ResponseHandlerInterface listener){
		if(url == null || url.length() == 0){
			throw new IllegalArgumentException("url is empty");
		}else if(!url.startsWith("http")){
			throw new IllegalArgumentException("the url "+url+", is not http or https");
		}else if(listener == null){
			throw new IllegalArgumentException("ResponseHandler must not be null");
		}
		HttpURLConnection connection = null;
		try {
			String requestUrl = url;
			if(params != null){
				requestUrl += requestUrl.contains("?") ? "&" : "?";
				requestUrl += params.getParams(isUrlEncodingEnabled);
			}
			connection = initUrlConnection(new URL(requestUrl));
			connection.setDoInput(true);
			connection.setDoOutput(false);
			connection.setRequestMethod(RequestMethod.GET);
			RequestExecute requestExecute = new RequestExecute(connection, params, listener);
			requestExecute.setCookieManager(mCookieManager);

			if(listener.getUseSynchronousMode()){
				requestExecute.run();
			}else{
				threadPool.execute(requestExecute);
			}
			RequestHandle requestHandle = new RequestHandle(requestExecute);

			if(context != null){
				List<RequestHandle> requestList = requestMap.get(context);
				synchronized (requestMap) {
					if (requestList == null){
						requestList = Collections.synchronizedList(new LinkedList<RequestHandle>());
						requestMap.put(context, requestList);
					}
				}
				requestList.add(requestHandle);

				Iterator<RequestHandle> iterator = requestList.iterator();
				while (iterator.hasNext()) {
					if (iterator.next().shouldBeGarbageCollected()) {
						iterator.remove();
					}
				}
			}
			return requestHandle;
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("the url " + url + " is not legitimate");
		}
	}

	public RequestHandle post(String url, RequestParams params, ResponseHandlerInterface listener){
		return post(null, url, params, listener);
	}

	public RequestHandle post(Context context, String url, RequestParams params, ResponseHandlerInterface listener){
		if(url == null || url.length() == 0){
			throw new RuntimeException("url is empty");
		}else if(!url.startsWith("http")){
			throw new RuntimeException("the url "+url+", is not http or https");
		}else if(listener == null){
			throw new IllegalArgumentException("ResponseHandler must not be null");
		}

		HttpURLConnection connection = null;
		try {
			if (params != null && params.isContentTypeJson()){
				setHeader(RequestParams.CONTENT_TYPE, RequestParams.CONTENT_TYPE_JSON
						+ ";charset=" + RequestParams.ENCODING);
			}else if(params != null){
				setHeader(RequestParams.CONTENT_TYPE, RequestParams.CONTENT_TYPE_MULTIPART_FORM_DATA
						+ ";boundary=" + params.getBoundary());
			}

			connection = initUrlConnection(new URL(url));
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setRequestMethod(RequestMethod.POST);
			RequestExecute requestExecute = new RequestExecute(connection, params, listener);
			requestExecute.setCookieManager(mCookieManager);

			if(listener.getUseSynchronousMode()){
				requestExecute.run();
			}else{
				threadPool.execute(requestExecute);
			}

			RequestHandle requestHandle = new RequestHandle(requestExecute);
			if(context != null){
				List<RequestHandle> requestList = requestMap.get(context);
				synchronized (requestMap) {
					if (requestList == null){
						requestList = Collections.synchronizedList(new LinkedList<RequestHandle>());
						requestMap.put(context, requestList);
					}
				}
				requestList.add(requestHandle);

				Iterator<RequestHandle> iterator = requestList.iterator();
				while (iterator.hasNext()) {
					if (iterator.next().shouldBeGarbageCollected()) {
						iterator.remove();
					}
				}
			}
			return requestHandle;
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("the url " + url + " is not legitimate");
		}
	}

	/**
	 * Sets the SSLSocketFactory to user when making requests. By default, a new, default
	 * SSLSocketFactory is used.
	 *
	 * @param sslSocketFactory the socket factory to use for https requests.
	 */
	public void setSSLSocketFactory(SSLSocketFactory sslSocketFactory) {
		this.mSSLSocketFactory = sslSocketFactory;
	}

	public void setCookieStore(CookieStore cookieStore){
		this.mCookieManager = new CookieManager(cookieStore, CookiePolicy.ACCEPT_ALL);
	}

}
