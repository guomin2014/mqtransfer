package com.gm.mqtransfer.facade.filter;

public enum ConvertMode {
	CUSTOM;
	
	public static ConvertMode getByValue(String value) {
		if (value == null || value.trim().length() == 0) {
			return null;
		}
		value = value.trim();
		for (ConvertMode mode : values()) {
			if (mode.name().equalsIgnoreCase(value)) {
				return mode;
			}
		}
		return null;
	}
}
