package com.zlq.zhttpclient.library;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by zhanglq on 15/5/31.
 */
public abstract class FileResponseListener extends ResponseHandlerImp {

	public static final String TAG = "FileResponseListener";
	private File mFile;

	public abstract void onSuccess(int responseCode, File file);

	public abstract void onFailure(int responseCode, File file, Throwable throwable);

	public FileResponseListener(File file) {
		super();
		assert (file != null);
		this.mFile = file;
	}

	public FileResponseListener(Context context) {
		super();
		this.mFile = getTemporaryFile(context);
	}

	public boolean deleteTargetFile() {
		return getTargetFile() != null && getTargetFile().delete();
	}

	protected File getTemporaryFile(Context context) {
		assert (context != null);
		try {
			return File.createTempFile("temp_", "_handled", context.getCacheDir());
		} catch (IOException e) {
			Log.e(TAG, "Cannot create temporary file", e);
		}
		return null;
	}

	/**
	 * Retrieves File object in which the response is stored
	 *
	 * @return File file in which the response is stored
	 */
	protected File getTargetFile() {
		assert (mFile != null);
		return mFile;
	}

	@Override
	byte[] getResponseData(int contentLength, InputStream inputStream) throws IOException {
		FileOutputStream buffer = new FileOutputStream(getTargetFile());
		buffer = new FileOutputStream(getTargetFile());
		byte[] tmp = new byte[1024];
		int l, currentLen = 0;
		// do not send messages if request has been cancelled
		while ((l = inputStream.read(tmp)) != -1 && !Thread.currentThread().isInterrupted()) {
			currentLen += l;
			buffer.write(tmp, 0, l);
			sendProgressMessage((int)((float)currentLen/(float)contentLength * 100f));
		}
		buffer.flush();
		buffer.close();
		inputStream.close();

		return null;
	}

	@Override
	public void onSuccess(int responseCode, byte[] responseContent) {
		onSuccess(responseCode, getTargetFile());
	}

	@Override
	public void onFailure(int responseCode, byte[] responseContent, Throwable throwable) {
		onFailure(responseCode, getTargetFile(), throwable);
	}
}
