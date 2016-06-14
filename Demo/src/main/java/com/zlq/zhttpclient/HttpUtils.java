package com.zlq.zhttpclient;

import android.content.Context;
import android.os.Environment;
import android.provider.SyncStateContract;

import com.zlq.zhttpclient.library.FileResponseListener;
import com.zlq.zhttpclient.library.RequestHandle;
import com.zlq.zhttpclient.library.RequestParams;
import com.zlq.zhttpclient.library.TextResponseListener;
import com.zlq.zhttpclient.library.ZHttpClient;

import java.io.File;
import java.io.IOException;

/**
 * Created by zhanglq on 16/6/14.
 */
public class HttpUtils {
	private ZHttpClient mZHttpClient;
	private Context mContext;

	public HttpUtils(Context context){
		this.mContext = context;
		mZHttpClient = new ZHttpClient();
	}

	class SyncRequestListener extends ResponseHandler{
		Object obj = null;

		public Object getResult(){
			return this.obj;
		}
		@Override
		public void onSuccess(int statusCode, Object obj) {
			this.obj = obj;
		}
		@Override
		public void onFailure(int statusCode, String responseContent) {
			this.obj = null;
		}
	}

	public File downloadFile(String url, String targetPath){
		final SyncRequestListener listener = new SyncRequestListener();
		FileResponseListener fileResponseListener = new FileResponseListener(new File(targetPath)) {
			@Override
			public void onSuccess(int responseCode, File file) {
				listener.onSuccess(responseCode, file);
			}
			@Override
			public void onFailure(int responseCode, File file, Throwable throwable) {
				listener.onFailure(responseCode, throwable == null ? "" : throwable.getLocalizedMessage());
			}
		};
		fileResponseListener.setUseSynchronousMode(true);
		mZHttpClient.get(url, null, fileResponseListener);
		File result = (File)listener.getResult();
		return result;
	}

	public String getData(String url){
		final SyncRequestListener listener = new SyncRequestListener();
		TextResponseListener textResponseListener = new TextResponseListener() {
			@Override
			public void onSuccess(int responseCode, String responseContent) {
				listener.onSuccess(responseCode, responseContent);
			}

			@Override
			public void onFailure(int responseCode, String responseContent, Throwable throwable) {
				listener.onFailure(responseCode, responseContent);
			}
		};
		textResponseListener.setUseSynchronousMode(true);
		mZHttpClient.get(url, null, textResponseListener);
		return (String)listener.getResult();
	}

	public RequestHandle get(String url, RequestParams params, TextResponseListener listener){
		return mZHttpClient.get(url, params, listener);
	}

	public RequestHandle post(String url, RequestParams params, TextResponseListener listener){
		return mZHttpClient.post(url, params, listener);
	}
}
