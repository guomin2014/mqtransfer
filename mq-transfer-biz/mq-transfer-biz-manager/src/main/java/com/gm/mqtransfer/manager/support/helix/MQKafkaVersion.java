package com.gm.mqtransfer.manager.support.helix;

public enum MQKafkaVersion {
	K_V_0_8_2,K_V_2_0_0;
	
	public static MQKafkaVersion getByValue(String value) {
		if (value == null || value.trim().length() == 0) {
			return null;
		}
		for (MQKafkaVersion type : values()) {
			if (type.name().equalsIgnoreCase(value)) {
				return type;
			}
		}
		return null;
	}
}
