package com.gm.mqtransfer.worker.config;

public class TransferLogConfiguration {

	/** 限制日志打印开关 */
	private boolean limitLogPrintEnable = true;
	/** 限制日志打印最小间隔时间 */
	private long limitLogPrintIntervalMs = 60000;
	/** 限制日志打印最大间隔次数 */
	private int limitLogPrintIntervalTimes = 100;
	/** 统计日志打印最小间隔时间 */
	private long statLogPrintIntervalMs = 60000;
	
	public boolean getLimitLogPrintEnable() {
		return limitLogPrintEnable;
	}
	public void setLimitLogPrintEnable(boolean limitLogPrintEnable) {
		this.limitLogPrintEnable = limitLogPrintEnable;
	}
	public long getLimitLogPrintIntervalMs() {
		return limitLogPrintIntervalMs;
	}
	public void setLimitLogPrintIntervalMs(long limitLogPrintIntervalMs) {
		this.limitLogPrintIntervalMs = limitLogPrintIntervalMs;
	}
	public int getLimitLogPrintIntervalTimes() {
		return limitLogPrintIntervalTimes;
	}
	public void setLimitLogPrintIntervalTimes(int limitLogPrintIntervalTimes) {
		this.limitLogPrintIntervalTimes = limitLogPrintIntervalTimes;
	}
	public long getStatLogPrintIntervalMs() {
		return statLogPrintIntervalMs;
	}
	public void setStatLogPrintIntervalMs(long statLogPrintIntervalMs) {
		this.statLogPrintIntervalMs = statLogPrintIntervalMs;
	}
	
}
