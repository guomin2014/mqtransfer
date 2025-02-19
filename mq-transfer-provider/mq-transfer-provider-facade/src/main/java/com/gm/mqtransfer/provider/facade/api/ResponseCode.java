package com.gm.mqtransfer.provider.facade.api;

public enum ResponseCode {

	Success("success", "操作成功"),
	CreateConsumerError("Failed to create consumer client", "创建消费客户端失败"),
	FetchOffsetError("Failed to fetch offset", "获取消费位置点失败"),
	CommitOffsetError("Failed to commit offset", "提交消费位置点失败"),
	StartOffsetEmptyError("Start offset cannot be empty", "开始消费位置点不能为空"),
	ConsumerClientNotFound("Consumer client does not exists", "消费客户端不存在"),
	FetchMessageError("Failed to fetch message", "获取消息失败"),
	SendMessageTimeoutError("Sending message timeout", "发送消息超时"),
	NotLeaderForPartitionError("Leader not available", "分区的Leader不存在"),
	PartitionEmptyError("Partition cannot be empty", "分区不存在"),
	OffsetOutOfRangeError("Offset out of range", "位置点不在正确的范围内"),
	AssignPartitionError("Failed to assign partition", "订阅分区失败"),
	MessageBodyEmptyError("Message body cannot be empty", "消息内容不能为空"),
	MessageBodyOutOfRangeError("Message body size out of range", "消息内容长度超过限制"),
	MessageConvertError("Failed to convert message", "消息转换错误"),
	OtherError("Other error", "其它异常"),
	;
	
	private String msg;
	private String desc;
	
	public String getMsg() {
		return msg;
	}

	public String getDesc() {
		return desc;
	}

	ResponseCode(String msg, String desc) {
		this.msg = msg;
		this.desc = desc;
	}
	
	public static ResponseCode getByName(String name) {
		if (name == null || name.trim().length() == 0) {
			return null;
		}
		for (ResponseCode type : values()) {
			if (type.name().equalsIgnoreCase(name)) {
				return type;
			}
		}
		return null;
	}
}
