package com.gm.mqtransfer.facade.model;

public class TaskMessage {

	private String taskCode;
	
	private String fromPartitionKey;
	
	public String getTaskCode() {
		return taskCode;
	}

	public void setTaskCode(String taskCode) {
		this.taskCode = taskCode;
	}

	public String getFromPartitionKey() {
		return fromPartitionKey;
	}

	public void setFromPartitionKey(String fromPartitionKey) {
		this.fromPartitionKey = fromPartitionKey;
	}

	@Override
	public String toString() {
		return "TaskMessage [taskCode=" + this.getTaskCode()
		+ ",fromPartitionKey=" + this.getFromPartitionKey() + "]";
	}
	
}
