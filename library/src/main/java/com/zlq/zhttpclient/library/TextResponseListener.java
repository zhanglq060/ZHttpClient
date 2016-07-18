package com.zlq.zhttpclient.library;

import android.util.Log;

import java.io.UnsupportedEncodingException;

/**
 * Created by zhanglq on 15/5/31.
 */
public abstract class TextResponseListener extends ResponseHandlerImp {

	public static final String TAG = "TextResponseListener";

	public abstract void onSuccess(int responseCode, String responseContent);

	public abstract void onFailure(int responseCode, String responseContent, Throwable throwable);


	@Override
	public void onSuccess(int responseCode, byte[] responseContent) {
		String responseText = null;
		try {
			responseText = responseContent == null ? null : new String(responseContent, getCharset());

		} catch (UnsupportedEncodingException e) {
			Log.e(TAG, "Encoding response into string failed", e);
		}
		onSuccess(responseCode, responseText);
	}

	@Override
	public void onFailure(int responseCode, byte[] responseContent, Throwable throwable) {
		String responseText = null;
		try {
			responseText = responseContent == null ? null : new String(responseContent, getCharset());
		} catch (UnsupportedEncodingException e) {
			Log.e(TAG, "Encoding response into string failed", e);
		}
		onFailure(responseCode, responseText, throwable);
	}
}
