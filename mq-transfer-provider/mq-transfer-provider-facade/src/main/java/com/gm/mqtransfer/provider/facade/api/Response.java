package com.gm.mqtransfer.provider.facade.api;

public class Response<T> {

	private String code;
	
	private String msg;
	
	private T data;
	
	public Response() {}
	
	public Response(String code, String msg) {
		this.code = code;
		this.msg = msg;
	}
	public Response(String code, String msg, T data) {
		this.code = code;
		this.msg = msg;
		this.data = data;
	}
	
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public String getMsg() {
		return msg;
	}
	public void setMsg(String msg) {
		this.msg = msg;
	}
	public T getData() {
		return data;
	}
	public void setData(T data) {
		this.data = data;
	}
	
	public boolean isSuccess() {
		return this.code != null && this.code.equalsIgnoreCase(ResponseCode.Success.name());
	}
	
	@Override
	public String toString() {
		return "Response [code=" + code + ", msg=" + msg + ", data=" + data + "]";
	}

}
