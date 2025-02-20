package com.gm.mqtransfer.manager.support.helix;

public enum MQType {
	KAFKA, ROCKETMQ;
	
	public static MQType getByValue(String value) {
		if (value == null || value.trim().length() == 0) {
			return null;
		}
		for (MQType type : values()) {
			if (type.name().equalsIgnoreCase(value)) {
				return type;
			}
		}
		return null;
	}
}
