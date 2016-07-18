package com.zlq.zhttpclient.library;


import java.io.IOException;
import java.io.InputStream;

/**
 * Created by zhanglq on 15/5/31.
 */
public interface ResponseHandlerInterface {

	void sendStartMessage();

	void sendFinishMessage();

	void sendProgressMessage(int progress);

	void sendCancelMessage();

	void sendResponseMessage(int responseCode, int contentLength, InputStream inputStream) throws IOException;

	void sendSuccessMessage(int responseCode, byte[] responseBody);

	void sendFailureMessage(int responseCode, byte[] responseBody, Throwable error);

	void setUseSynchronousMode(boolean useSynchronousMode);

	boolean getUseSynchronousMode();
}
