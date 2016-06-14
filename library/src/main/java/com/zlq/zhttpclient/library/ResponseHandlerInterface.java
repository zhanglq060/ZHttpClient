package com.zlq.zhttpclient.library;

/**
 * Created by zhanglq on 15/5/31.
 */
public interface ResponseHandlerInterface {

	void sendStartMessage();

	void sendFinishMessage();

	void sendProgressMessage(int progress);

	void sendCancelMessage();

	void sendSuccessMessage(int statusCode, byte[] responseBody);

	void sendFailureMessage(int statusCode, byte[] responseBody, Throwable error);

	void setUseSynchronousMode(boolean useSynchronousMode);

	boolean getUseSynchronousMode();
}
