package com.gm.mqtransfer.provider.facade.service.producer;

import com.gm.mqtransfer.provider.facade.api.ResponseCode;

public class ResultMetadata {
	/** 状态码，详见：com.gm.mqtransfer.provider.facade.api.ResponseCode */
	private final String code;
	/** 描述信息 */
	private String msg;
	
	private Long offset;
	
	private String msgId;
	
	public ResultMetadata(String code, String msg) {
		this.code = code;
		this.msg = msg;
	}
	public ResultMetadata(String code, String msg, Long offset, String msgId) {
		this.code = code;
		this.msg = msg;
		this.offset = offset;
		this.msgId = msgId;
	}

	public String getCode() {
		return code;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public Long getOffset() {
		return offset;
	}

	public String getMsgId() {
		return msgId;
	}

	@Override
	public String toString() {
		StringBuilder build = new StringBuilder();
		build.append("ResultMetadata[");
		build.append("code").append("=").append(code);
		build.append(",").append("msg").append("=").append(msg);
		if (offset != null) {
			build.append(",").append("offset").append("=").append(offset);
		}
		if (msgId != null) {
			build.append(",").append("msgId").append("=").append(msgId);
		}
		build.append("]");
		return build.toString();
	}
	
	public static ResultMetadata createFailureResult(String code, String msg) {
		return new ResultMetadata(code, msg);
	}
	public static ResultMetadata createSuccessResult(Long offset, String msgId) {
		return new ResultMetadata(ResponseCode.Success.name(), ResponseCode.Success.getMsg(), offset, msgId);
	}
}
