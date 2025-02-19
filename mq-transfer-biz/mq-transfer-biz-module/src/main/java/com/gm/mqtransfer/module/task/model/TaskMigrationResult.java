package com.gm.mqtransfer.module.task.model;

public enum TaskMigrationResult {
	SUCCESS(0),
	NOTEXISTS(1),
	NOTLOCK(2),
	OTHER(9),
	;
	
	private int value;
	
	TaskMigrationResult(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	
}
