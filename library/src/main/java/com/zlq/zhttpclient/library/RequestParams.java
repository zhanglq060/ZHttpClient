package com.zlq.zhttpclient.library;

import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by zhanglq on 15/5/30.
 */
public class RequestParams {

	public static final String ENCODING = "UTF-8";
	public static final String CONTENT_TYPE_JSON = "application/json";

	private boolean isContentTypeJson = false;

	private ConcurrentHashMap<String, String> stringParams = new ConcurrentHashMap<String, String>();
	private ConcurrentHashMap<String, Byte[]> byteParams = new ConcurrentHashMap<String, Byte[]>();
	private ConcurrentHashMap<String, File> fileParams = new ConcurrentHashMap<String, File>();

	public void put(String key, int value){
		stringParams.put(key, value+"");
	}
	public void put(String key, String value){
		stringParams.put(key, value);
	}

	public void put(String key, Byte[] value){
		byteParams.put(key, value);
	}

	public void put(String key, File value){
		fileParams.put(key, value);
	}

	public void setContentTypeJson(boolean isJson){
		this.isContentTypeJson = isJson;
	}

	@Override
	public String toString(){
		return null;
	}

	public String getParams(boolean urlencoding){
		Iterator iterator = stringParams.entrySet().iterator();
		String paramsString = "";
		while (iterator.hasNext()){
			Map.Entry<String, String> entry = (Map.Entry<String, String>) iterator.next();
			String key = entry.getKey();
			String value = urlencoding ? entry.getValue().replace(" ", "%20") : entry.getValue();
			paramsString += paramsString.length() == 0 ? "" : "&";
			paramsString += key;
			paramsString += "=";
			paramsString += value;
		}
		return paramsString;
	}

	public String getJsonParams(){
		String string = "{";
		for (Iterator it = stringParams.entrySet().iterator(); it.hasNext();) {
			Map.Entry e = (Map.Entry) it.next();
			string += "'" + e.getKey() + "':";
			string += "'" + e.getValue() + "',";
		}
		string = string.substring(0, string.lastIndexOf(","));
		string += "}";
		return string;
	}

	public ConcurrentHashMap<String, String> getPostStringParams(){
		return stringParams;
	}

	public ConcurrentHashMap<String, Byte[]> getByteParams(){
		return byteParams;
	}

	public ConcurrentHashMap<String, File> getFileParams(){
		return fileParams;
	}

	public void preparePostParams(URLConnection connection) throws IOException {
		String BOUNDARY = java.util.UUID.randomUUID().toString();
		String MULTIPART_FROM_DATA = "multipart/form-data";
		String PREFIX = "--";
		String NEWLINE = "\r\n";
		ConcurrentHashMap<String, String> stringParams = getPostStringParams();//text
		ConcurrentHashMap<String, Byte[]> byteParams = getByteParams();//byte
		ConcurrentHashMap<String, File> fileParams = getFileParams();//file

		if(byteParams.size() > 0 || fileParams.size() > 0){
			connection.setRequestProperty("Content-Type", MULTIPART_FROM_DATA + ";boundary=" + BOUNDARY);
		}else if(isContentTypeJson){
			connection.setRequestProperty("Content-Type", CONTENT_TYPE_JSON+";charset="+ENCODING);
		}

		DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());

		//text
		if(stringParams != null && stringParams.size() > 0){
			String stringParamsText = isContentTypeJson ? getJsonParams() : getParams(false);
			outputStream.write(stringParamsText.getBytes());
			outputStream.flush();
		}

		//byte
		if(byteParams != null && byteParams.size() > 0){
			Iterator iterator = byteParams.entrySet().iterator();
			while (iterator.hasNext()){
				Map.Entry<String, byte[]> entry = (Map.Entry<String, byte[]>) iterator.next();
				StringBuilder byteparamsText = new StringBuilder();
				byteparamsText.append(PREFIX);
				byteparamsText.append(BOUNDARY);
				byteparamsText.append(NEWLINE);
				byteparamsText.append("Content-Disposition: form-data; name=\""
						+ entry.getKey() + "\"; filename=\""
						+ entry.getKey() + "\"" + NEWLINE);
				byteparamsText.append("Content-Type: application/octet-stream" + NEWLINE);
				byteparamsText.append(NEWLINE);
				outputStream.write(byteparamsText.toString().getBytes());

				InputStream is = new ByteArrayInputStream(entry.getValue());
				byte[] buffer = new byte[1024];
				int len = 0;
				while ((len = is.read(buffer)) != -1) {
					outputStream.write(buffer, 0, len);
				}
				is.close();
				outputStream.write(NEWLINE.getBytes());
				outputStream.flush();
			}
		}

		//file
		if(fileParams != null && fileParams.size() > 0){
			Iterator iterator = fileParams.entrySet().iterator();
			while (iterator.hasNext()){
				Map.Entry<String, File> entry = (Map.Entry<String, File>) iterator.next();
				StringBuilder fileparamsText = new StringBuilder();
				fileparamsText.append(PREFIX);
				fileparamsText.append(BOUNDARY);
				fileparamsText.append(NEWLINE);
				fileparamsText.append("Content-Disposition: form-data; name=\""
						+ entry.getKey() + "\"; filename=\""
						+ entry.getValue() + "\"" + NEWLINE);
				fileparamsText.append("Content-Type: application/octet-stream" + NEWLINE);
				fileparamsText.append(NEWLINE);
				outputStream.write(fileparamsText.toString().getBytes());

				InputStream is = new FileInputStream(entry.getValue());
				byte[] buffer = new byte[1024];
				int len = 0;
				while ((len = is.read(buffer)) != -1) {
					outputStream.write(buffer, 0, len);
				}
				is.close();
				outputStream.write(NEWLINE.getBytes());
				outputStream.flush();
			}
		}

		if(fileParams.size() > 0 || byteParams.size() > 0){
			//请求结束标志
			byte[] end_data = (PREFIX + BOUNDARY + PREFIX + NEWLINE).getBytes();
			outputStream.write(end_data);
		}
		outputStream.close();
	}
}