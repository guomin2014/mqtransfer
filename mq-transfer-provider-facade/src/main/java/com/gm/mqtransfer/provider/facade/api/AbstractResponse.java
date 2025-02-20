package com.gm.mqtransfer.provider.facade.api;

public class AbstractResponse<T> extends Response<T>{

	public AbstractResponse(String code, String msg) {
		super(code, msg);
	}
	public AbstractResponse(String code, String msg, T data) {
		super(code, msg, data);
	}
}
