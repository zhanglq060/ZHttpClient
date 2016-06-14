package com.zlq.zhttpclient.library;

import java.lang.ref.WeakReference;

/**
 * Created by zhanglq on 15/6/14.
 */
public class RequestHandle {
	private final WeakReference<RequestExecute> request;

	public RequestHandle(RequestExecute request) {
		this.request = new WeakReference<RequestExecute>(request);
	}

	public boolean cancel() {
		RequestExecute _request = request.get();
		return _request == null || _request.cancle();
	}

	public boolean isFinished() {
		RequestExecute _request = request.get();
		return _request == null || _request.isDone();
	}

	public boolean isCancelled() {
		RequestExecute _request = request.get();
		return _request == null || _request.isCancle();
	}

	public boolean shouldBeGarbageCollected() {
		boolean should = isCancelled() || isFinished();
		if (should)
			request.clear();
		return should;
	}
}
