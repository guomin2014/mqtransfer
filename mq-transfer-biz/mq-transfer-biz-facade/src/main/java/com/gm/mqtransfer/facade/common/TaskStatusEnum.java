package com.gm.mqtransfer.facade.common;

public enum TaskStatusEnum {
	
	DISABLE(0),
	ENABLE(1);
	
	int value;
	
	TaskStatusEnum(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}
	
	public static TaskStatusEnum getByValue(Integer value) {
		if (value == null) {
			return null;
		}
		for (TaskStatusEnum ele : values()) {
			if (ele.getValue() == value.intValue()) {
				return ele;
			}
		}
		return null;
	}
}
