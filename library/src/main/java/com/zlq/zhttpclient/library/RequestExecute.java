package com.zlq.zhttpclient.library;

import android.os.Process;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.UnknownHostException;

/**
 * Created by zhanglq on 15/5/31.
 */
public class RequestExecute implements Runnable {

	public static final String TAG = "RequestExecute";

	public static final int DEFAULT_MAX_RETRIES = 5;
	public static final int DEFAULT_RETRY_SLEEP_TIME_MILLIS = 1500;
	public static final int DEFAULT_SOCKET_BUFFER_SIZE = 8192;

	private HttpURLConnection connection;
	private ResponseHandlerInterface mResponseHandler;
	private RequestParams mRequestParams;
	private int retryCount = 0;
	private boolean isCancle = false;
	public boolean isFinish = false;

	public RequestExecute(HttpURLConnection connection, RequestParams params, ResponseHandlerInterface responseHandler){
		this.connection = connection;
		this.mResponseHandler = responseHandler;
		this.mRequestParams = params;
	}

	@Override
	public void run() {
		Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

		if(isCancle()){
			mResponseHandler.sendCancelMessage();
			return;
		}

		mResponseHandler.sendStartMessage();

		try {
			makeRequestAndRetry();
		} catch (IOException e) {
			if (!isCancle()) {
				mResponseHandler.sendFailureMessage(0, null, e);
			} else {
				Log.e(TAG, "makeRequestAndRetry returned error, but handler is null", e);
			}
		}

		if(isCancle()){
			mResponseHandler.sendCancelMessage();
			return;
		}
		mResponseHandler.sendFinishMessage();
		isFinish = true;
		connection.disconnect();
	}

	private void makeRequestAndRetry() throws IOException {
		boolean retry = true;
		IOException cause = null;
		RetryHandler retryHandler = new RetryHandler(DEFAULT_MAX_RETRIES, DEFAULT_RETRY_SLEEP_TIME_MILLIS);
		try{
			while (retry){
				try {
					if(!isCancle()){
						makeRequest();
					}
					return;
				}catch (UnknownHostException e) {
					cause = new IOException("UnknownHostException exception: " + e.getMessage());
					retry = (retryCount > 0) && retryHandler.retryRequest(cause, ++retryCount);
				} catch (NullPointerException e) {
					cause = new IOException("NPE in HttpClient: " + e.getMessage());
					retry = retryHandler.retryRequest(cause, ++retryCount);
				} catch (IOException e) {
					cause = e;
					retry = retryHandler.retryRequest(cause, ++retryCount);
				}
			}
		}catch (Exception e){
			Log.e(TAG, "Unhandled exception origin cause", e);
			cause = new IOException("Unhandled exception: " + e.getMessage());
		}

		throw(cause);

	}

	private void makeRequest() throws IOException {

		int responseCode = -1;
		if(connection.getRequestMethod().equals(RequestMethod.POST)){
			mRequestParams.preparePostParams(connection);
		}
		responseCode = connection.getResponseCode();

		mResponseHandler.sendResponseMessage(responseCode, connection.getContentLength(), connection.getInputStream());
	}

	public boolean isCancle(){
		return this.isCancle;
	}

	public boolean cancle(){
		isCancle = true;
		connection.disconnect();
		return true;
	}

	public boolean isDone(){
		return isCancle || isFinish;
	}

}
