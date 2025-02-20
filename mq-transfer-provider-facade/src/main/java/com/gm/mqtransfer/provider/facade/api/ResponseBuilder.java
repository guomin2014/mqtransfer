package com.gm.mqtransfer.provider.facade.api;

public class ResponseBuilder<T extends Response> {

	private Class<T> type;
	
	private String code;
	
	private String msg;
	
	private Object data;
	
	public ResponseBuilder(Class<T> type) {
		this.type = type;
	}
	
	public ResponseBuilder<T> forCode(String code) {
		this.code = code;
		return this;
	}
	public ResponseBuilder<T> forCode(ResponseCode code) {
		this.code = code.name();
		this.msg = code.getMsg();
		return this;
	}
	public ResponseBuilder<T> forMsg(String msg) {
		this.msg = msg;
		return this;
	}
	public ResponseBuilder<T> forData(Object data) {
		this.data = data;
		return this;
	}
	
	public T build() {
		if (this.type == null) {
			return null;
		}
		try {
			T instance = this.type.newInstance();
			instance.setData(data);
			if (this.code != null) {
				instance.setCode(this.code);
				instance.setMsg(this.msg);
			} else {
				instance.setCode(ResponseCode.Success.name());
				instance.setMsg(ResponseCode.Success.getMsg());
			}
			return instance;
		} catch (Exception e) {
			return null;
		}
	}
	public static <E extends Response> E toBuild(Class<E> clazz, String code, String msg) {
		return new ResponseBuilder<E>(clazz).forCode(code).forMsg(msg).build();
	}
	public static <E extends Response> E toBuild(Class<E> clazz, ResponseCode code) {
		return new ResponseBuilder<E>(clazz).forCode(code).build();
	}
	public static <E extends Response> E toBuild(Class<E> clazz, Object data) {
		return new ResponseBuilder<E>(clazz).forData(data).build();
	}
}
