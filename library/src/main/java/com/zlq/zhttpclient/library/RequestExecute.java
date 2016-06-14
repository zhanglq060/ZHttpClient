package com.zlq.zhttpclient.library;

import android.os.Process;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.net.HttpURLConnection;
import java.net.UnknownHostException;
import java.util.zip.GZIPInputStream;

/**
 * Created by zhanglq on 15/5/31.
 */
public class RequestExecute implements Runnable{

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
		if(mResponseHandler != null){
			mResponseHandler.sendStartMessage();
		}

		try {
			makeRequestAndRetry();
		} catch (IOException e) {
			if (!isCancle() && mResponseHandler != null) {
				mResponseHandler.sendFailureMessage(0, null, e);
			} else {
				Log.e(TAG, "makeRequestAndRetry returned error, but handler is null", e);
			}
		}

		if(isCancle()){
			if(mResponseHandler != null) mResponseHandler.sendCancelMessage();
			return;
		}
		if(mResponseHandler != null) mResponseHandler.sendFinishMessage();
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

		byte[] content = responseReadHand(responseCode, connection.getContentLength(), connection.getInputStream());

		if(content == null || responseCode >= 300 || responseCode == -1){
			if(mResponseHandler != null) mResponseHandler.sendFailureMessage(responseCode, content, new RuntimeException("http server error"));
		}else{
			if(mResponseHandler != null) mResponseHandler.sendSuccessMessage(responseCode, content);
		}
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

	private byte[] responseReadHand(int responseCode, int contentLenght, InputStream inputStream) throws IOException {
		ByteArrayOutputStream outputStream = null;
		BufferedInputStream bufferedInputStream = null;
		try {
			bufferedInputStream = new BufferedInputStream(getInputSteam(inputStream));
			outputStream = new ByteArrayOutputStream(1024);
			byte[] buffer = new byte[1024];
			int len = 0;
			int currentLen = 0;
			while ((len = bufferedInputStream.read(buffer)) != -1){
				currentLen += len;
				outputStream.write(buffer, 0, len);
				if(mResponseHandler != null) mResponseHandler.sendProgressMessage((int)((float)currentLen/(float)contentLenght * 100f));
			}
			return outputStream.toByteArray();
		}catch (IOException e) {
			throw(e);
		}finally {
			try {
				bufferedInputStream.close();
				outputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw(e);
			}
		}
	}

	private InputStream getInputSteam(InputStream inputStream) throws IOException {
		PushbackInputStream pushbackStream = new PushbackInputStream(inputStream, 2);
		GZIPInputStream gzippedStream = null;
		if (isInputStreamGZIPCompressed(pushbackStream)) {
			gzippedStream = new GZIPInputStream(pushbackStream);
			return gzippedStream;
		} else {
			return pushbackStream;
		}
	}

	private boolean isInputStreamGZIPCompressed(final PushbackInputStream inputStream) throws IOException {
		if (inputStream == null)
			return false;

		byte[] signature = new byte[2];
		int readStatus = inputStream.read(signature);
		inputStream.unread(signature);
		int streamHeader = ((int) signature[0] & 0xff) | ((signature[1] << 8) & 0xff00);
		return readStatus == 2 && GZIPInputStream.GZIP_MAGIC == streamHeader;
	}
}
