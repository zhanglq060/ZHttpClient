package com.zlq.zhttpclient.library;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * Created by zhanglq on 15/5/31.
 */
public abstract class ResponseHandlerImp implements ResponseHandlerInterface{

	public static final String TAG = "ResponseHandlerImp";

	protected static final int SUCCESS_MESSAGE = 0;
	protected static final int FAILURE_MESSAGE = 1;
	protected static final int START_MESSAGE = 2;
	protected static final int FINISH_MESSAGE = 3;
	protected static final int PROGRESS_MESSAGE = 4;
	protected static final int CANCEL_MESSAGE = 5;

	public static final String DEFAULT_CHARSET = "UTF-8";
	private String responseCharset = DEFAULT_CHARSET;

	private Handler mHandler;
	private boolean useSynchronousMode;

	public ResponseHandlerImp(){
		setUseSynchronousMode(false);
	}

	protected abstract void onHandleSuccess(int responseCode, byte[] responseContent);
	protected abstract void onHandleFailure(int responseCode, byte[] responseContent, Throwable throwable);

	public void onProgress(int progressPercent){

	}

	public void onCancel(){

	}

	public void onFinish(){

	}

	public void onStart(){

	}

	@Override
	public void sendStartMessage() {
		sendMessage(obtainMessage(START_MESSAGE, null));
	}

	@Override
	public void sendFinishMessage() {
		sendMessage(obtainMessage(FINISH_MESSAGE, null));
	}

	@Override
	public void sendProgressMessage(int progress) {
		sendMessage(obtainMessage(PROGRESS_MESSAGE, new Object[]{progress}));
	}

	@Override
	public void sendCancelMessage() {
		sendMessage(obtainMessage(CANCEL_MESSAGE, null));
	}

	@Override
	public void sendSuccessMessage(int statusCode, byte[] responseBody) {
		sendMessage(obtainMessage(SUCCESS_MESSAGE, new Object[]{statusCode, responseBody}));
	}

	@Override
	public void sendFailureMessage(int statusCode, byte[] responseBody, Throwable error) {
		sendMessage(obtainMessage(SUCCESS_MESSAGE, new Object[]{statusCode, responseBody, error}));
	}

	public void setCharset(final String charset) {
		this.responseCharset = charset;
	}

	public String getCharset() {
		return this.responseCharset == null ? DEFAULT_CHARSET : this.responseCharset;
	}

	@Override
	public void setUseSynchronousMode(boolean value) {
// A looper must be prepared before setting asynchronous mode.
		if (!value && Looper.myLooper() == null) {
			value = true;
			Log.w(TAG, "Current thread has not called Looper.prepare(). Forcing synchronous mode.");
		}

		// If using asynchronous mode.
		if (!value && mHandler == null) {
			// Create a handler on current thread to submit tasks
			mHandler = new ResponderHandler(this);
		} else if (value && mHandler != null) {
			// TODO: Consider adding a flag to remove all queued messages.
			mHandler = null;
		}

		useSynchronousMode = value;
	}

	private static class ResponderHandler extends Handler{
		private ResponseHandlerImp mResponseHandlerImp;
		public ResponderHandler(ResponseHandlerImp imp){
			mResponseHandlerImp = imp;
		}

		@Override
		public void handleMessage(Message msg) {
			mResponseHandlerImp.handleMessage(msg);
		}
	}

	@Override
	public boolean getUseSynchronousMode() {
		return this.useSynchronousMode;
	}

	protected void handleMessage(Message message) {
		Object[] response;

		switch (message.what) {
			case SUCCESS_MESSAGE:
				response = (Object[]) message.obj;
				if (response != null && response.length >= 2) {
					onHandleSuccess((Integer) response[0], (byte[]) response[1]);
				} else {
					Log.e(TAG,"SUCCESS_MESSAGE didn't got enough params");
				}
				break;
			case FAILURE_MESSAGE:
				response = (Object[]) message.obj;
				if (response != null && response.length >= 3) {
					onHandleFailure((Integer) response[0], (byte[]) response[1], (Throwable) response[2]);
				} else {
					Log.e(TAG,"FAILURE_MESSAGE didn't got enough params");
				}
				break;
			case START_MESSAGE:
				onStart();
				break;
			case FINISH_MESSAGE:
				onFinish();
				break;
			case PROGRESS_MESSAGE:
				response = (Object[]) message.obj;
				if (response != null && response.length >= 1) {
					onProgress((Integer) response[0]);
				} else {
					Log.e(TAG,"PROGRESS_MESSAGE didn't got enough params");
				}
				break;
			case CANCEL_MESSAGE:
				onCancel();
				break;
		}
	}

	protected void sendMessage(Message msg) {
		if (getUseSynchronousMode() || mHandler == null) {
			handleMessage(msg);
		} else if (!Thread.currentThread().isInterrupted()) { // do not send messages if request has been cancelled
			mHandler.sendMessage(msg);
		}
	}

	protected void postRunnable(Runnable runnable) {
		if (runnable != null) {
			if (getUseSynchronousMode() || mHandler == null) {
				// This response handler is synchronous, run on current thread
				runnable.run();
			} else {
				// Otherwise, run on provided handler
				mHandler.post(runnable);
			}
		}
	}

	protected Message obtainMessage(int responseMessageId, Object responseMessageData) {
		Message msg;
		if (mHandler == null) {
			msg = Message.obtain();
			if (msg != null) {
				msg.what = responseMessageId;
				msg.obj = responseMessageData;
			}
		} else {
			msg = Message.obtain(mHandler, responseMessageId, responseMessageData);
		}
		return msg;
	}
}
