package com.gm.mqtransfer.module.task.model;

public enum TaskMigrationStatus {
	WAIT,STOPING,STOPED,STARTING,STARTED,COMPLETED;
	
	public static TaskMigrationStatus getByName(String name) {
		if (name == null || name.trim().length() == 0) {
			return null;
		}
		for (TaskMigrationStatus status : values()) {
			if (status.name().equalsIgnoreCase(name)) {
				return status;
			}
		}
		return null;
	}
}
