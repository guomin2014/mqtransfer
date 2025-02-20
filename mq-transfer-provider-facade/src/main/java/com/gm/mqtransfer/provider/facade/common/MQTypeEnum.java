package com.gm.mqtransfer.provider.facade.common;

public enum MQTypeEnum {

	Kafka,RocketMQ;
	
	public static MQTypeEnum getByName(String name) {
		if (name == null || name.trim().length() == 0) {
			return null;
		}
		for (MQTypeEnum type : values()) {
			if (type.name().equalsIgnoreCase(name)) {
				return type;
			}
		}
		return null;
	}
}
