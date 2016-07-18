package com.zlq.zhttpclient.library;

/**
 * Created by zhanglq on 15/5/31.
 */
public abstract class BytesResponseListener extends ResponseHandlerImp {

	public static final String TAG = "BytesResponseListener";

	public abstract void onSuccess(int responseCode, byte[] responseContent);

	public abstract void onFailure(int responseCode, byte[] responseContent, Throwable throwable);

}
