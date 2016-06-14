package com.zlq.zhttpclient;

public abstract class ResponseHandler {

	public void onStartWithCache(Object obj){
		
	}
	
	public void onStart(){
		
	}
	
	public abstract void onSuccess(int statusCode, Object obj);
	
	public abstract void onFailure(int statusCode, String responseContent);
	
	public void onFinish(){
		
	}
	
	public void onCancle(){
		
	}
}
