package com.gm.mqtransfer.provider.facade.model;

public class ServiceDesc {

	private String mqName;
	
	private String mqType;
	
	private String mqVersion;

	public String getMqName() {
		return mqName;
	}

	public void setMqName(String mqName) {
		this.mqName = mqName;
	}

	public String getMqType() {
		return mqType;
	}

	public void setMqType(String mqType) {
		this.mqType = mqType;
	}

	public String getMqVersion() {
		return mqVersion;
	}

	public void setMqVersion(String mqVersion) {
		this.mqVersion = mqVersion;
	}

	@Override
	public String toString() {
		return "mqName=" + mqName + ", mqType=" + mqType + ", mqVersion=" + mqVersion;
	}
	
}
