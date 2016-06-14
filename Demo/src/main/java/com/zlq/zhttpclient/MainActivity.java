package com.zlq.zhttpclient;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.zlq.zhttpclient.library.TextResponseListener;

import java.io.File;

public class MainActivity extends Activity implements View.OnClickListener{

	Button getBtn;
	Button postBtn;
	Button downloadBtn;
	TextView textView;
	ImageView imageView;

	HttpUtils mHttpUtils;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mHttpUtils = new HttpUtils(this);
		getBtn = (Button) findViewById(R.id.request_get_btn);
		postBtn = (Button) findViewById(R.id.request_post_btn);
		downloadBtn = (Button) findViewById(R.id.request_downloadfile_btn);
		textView = (TextView) findViewById(R.id.text);
		imageView = (ImageView) findViewById(R.id.image);

		getBtn.setOnClickListener(this);
		postBtn.setOnClickListener(this);
		downloadBtn.setOnClickListener(this);
	}

	ProgressDialog progressDialog = null;
	static Handler handler = new Handler();
	@Override
	public void onClick(View v) {
		textView.setText("");
		switch (v.getId()){
			case R.id.request_get_btn:
				mHttpUtils.get("https://www.baidu.com", null, new TextResponseListener() {
					@Override
					public void onStart() {
						super.onStart();
						if(progressDialog == null) progressDialog = new ProgressDialog(MainActivity.this);
						progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
						progressDialog.setMessage("加载中...");
						progressDialog.show();
					}

					@Override
					public void onSuccess(int responseCode, String responseContent) {
						textView.setVisibility(View.VISIBLE);
						imageView.setVisibility(View.GONE);
						textView.setText(responseContent);
					}
					@Override
					public void onFailure(int responseCode, String responseContent, Throwable throwable) {
						textView.setText(responseContent);
					}

					@Override
					public void onFinish() {
						super.onFinish();
						progressDialog.dismiss();
					}

					@Override
					public void onCancel() {
						super.onCancel();
						progressDialog.dismiss();
					}
				});
				break;
			case R.id.request_post_btn:

				break;
			case R.id.request_downloadfile_btn:
//http://f.hiphotos.baidu.com/image/pic/item/f636afc379310a5566becb8fb24543a982261036.jpg
				if(progressDialog == null) progressDialog = new ProgressDialog(MainActivity.this);
				progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				progressDialog.setMessage("加载中...");
				progressDialog.show();
				new Thread(new Runnable() {
					@Override
					public void run() {
						final File resultFile = mHttpUtils.downloadFile(
								"http://f.hiphotos.baidu.com/image/pic/item/f636afc379310a5566becb8fb24543a982261036.jpg",
								"/sdcard/zhttpclient_meinv.jpg");
						handler.post(new Runnable() {
							@Override
							public void run() {
								if(resultFile != null && resultFile.exists()){
									textView.setVisibility(View.GONE);
									imageView.setVisibility(View.VISIBLE);
									imageView.setImageBitmap(BitmapFactory.decodeFile(resultFile.getPath()));
								}
								progressDialog.dismiss();
							}
						});
					}
				}).start();

				break;
		}
	}
}
