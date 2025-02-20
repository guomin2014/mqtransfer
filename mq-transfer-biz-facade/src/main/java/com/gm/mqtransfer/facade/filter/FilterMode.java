package com.gm.mqtransfer.facade.filter;

public enum FilterMode {
	HEADER,CUSTOM;
	
	public static FilterMode getByValue(String value) {
		if (value == null || value.trim().length() == 0) {
			return null;
		}
		value = value.trim();
		for (FilterMode mode : values()) {
			if (mode.name().equalsIgnoreCase(value)) {
				return mode;
			}
		}
		return null;
	}
}
