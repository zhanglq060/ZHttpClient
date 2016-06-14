package com.zlq.zhttpclient.library;

import android.content.Context;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

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

	/**
	 * Obtains new FileAsyncHttpResponseHandler against context with target being temporary file
	 *
	 * @param context Context, must not be null
	 */
	public FileResponseListener(Context context) {
		super();
		this.mFile = getTemporaryFile(context);
	}

	/**
	 * Attempts to delete file with stored response
	 *
	 * @return false if the file does not exist or is null, true if it was successfully deleted
	 */
	public boolean deleteTargetFile() {
		return getTargetFile() != null && getTargetFile().delete();
	}

	/**
	 * Used when there is no file to be used when calling constructor
	 *
	 * @param context Context, must not be null
	 * @return temporary file or null if creating file failed
	 */
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
	protected void onHandleSuccess(int responseCode, byte[] responseContent) {
		ByteArrayInputStream instream = new ByteArrayInputStream(responseContent);
		FileOutputStream buffer = null;
		Throwable throwable = null;
		try {
			buffer = new FileOutputStream(getTargetFile());
			byte[] tmp = new byte[1024];
			int l, count = 0;
			// do not send messages if request has been cancelled
			while ((l = instream.read(tmp)) != -1 && !Thread.currentThread().isInterrupted()) {
				count += l;
				buffer.write(tmp, 0, l);
			}
			buffer.flush();
		}catch (Exception e){
			e.printStackTrace();
			throwable = e;
		}finally {
			try {
				buffer.close();
			} catch (IOException e) {
				e.printStackTrace();
				throwable = e;
			}
		}
		if(getTargetFile() != null && getTargetFile().exists()){
			onSuccess(responseCode, getTargetFile());
		}else{
			onFailure(responseCode, null, throwable);
		}

	}

	@Override
	protected void onHandleFailure(int responseCode, byte[] responseContent, Throwable throwable) {
		onFailure(responseCode, null, throwable);
	}
}
