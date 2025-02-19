package com.gm.mqtransfer.facade.model;

public class TransferTaskConvert {
	private boolean enable;
	private String mode;
	private String handler;
	private String rule;
	public boolean isEnable() {
		return enable;
	}
	public void setEnable(boolean enable) {
		this.enable = enable;
	}
	public String getMode() {
		return mode;
	}
	public void setMode(String mode) {
		this.mode = mode;
	}
	public String getHandler() {
		return handler;
	}
	public void setHandler(String handler) {
		this.handler = handler;
	}
	public String getRule() {
		return rule;
	}
	public void setRule(String rule) {
		this.rule = rule;
	}
	@Override
	public String toString() {
		return "TransferTaskConvert [enable=" + enable + ", mode=" + mode + ", handler=" + handler + ", rule=" + rule
				+ "]";
	}
	
}
