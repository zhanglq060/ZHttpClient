# ZHttpClient
基于HttpURLConnection的网络请求库，包括get和post请求 上传文件 下载文件 https 公钥私钥 GZIP压缩，等功能。
使用非常简单：

	ZHttpClient client = new ZHttpClient();
	client.get("https://www.baidu.com", null, new TextResponseListener() {
		@Override
		public void onStart() {
			super.onStart();
		}

		@Override
		public void onSuccess(int responseCode, String responseContent) {
		}
		@Override
		public void onFailure(int responseCode, String responseContent, Throwable throwable) {
		}
		@Override
		public void onFinish() {
			super.onFinish();
		}

		@Override
		public void onCancel() {
			super.onCancel();
		}
	});
