package com.gm.mqtransfer.facade.model;

import java.util.List;

/**
 * 生产者配置
 * @author GM
 * @date 2022-07-04
 */
public class ProducerConfig {
	/** 共享生产者客户端，默认：不共享 */
	private boolean clientShare = false;
	/** 生产批量发送最大可发送记录数 */
	private int maxBatchRecords = 1000;
	/** 生产批量发送最大可发送大小，单位：byte */
	private long maxBatchSize = 8388608;//8M
	/** 生产批量发送单条最大可发送大小，单位：byte */
	private long maxSingleSize = 8388608;//8M
	/** 生产发送超时时间 */
	private long sendTimeoutMs = 120000;
	/** 生产消息最大暂停时间，单位：毫秒 */
	private long maxSuspendTime = 2000;//2s
	/** 目标生产转换配置 */
	private List<TransferTaskConvert> convertConfigs;
	/** 原生生产客户端配置 */
	private String clientConfig;
	/** 日志级别 */
	private String logLevel = "ERROR";
	
	public boolean isClientShare() {
		return clientShare;
	}
	public void setClientShare(boolean clientShare) {
		this.clientShare = clientShare;
	}
	public String getClientConfig() {
		return clientConfig;
	}
	public void setClientConfig(String clientConfig) {
		this.clientConfig = clientConfig;
	}
	public int getMaxBatchRecords() {
		return maxBatchRecords;
	}
	public void setMaxBatchRecords(int maxBatchRecords) {
		this.maxBatchRecords = maxBatchRecords;
	}
	public long getMaxBatchSize() {
		return maxBatchSize;
	}
	public void setMaxBatchSize(long maxBatchSize) {
		this.maxBatchSize = maxBatchSize;
	}
	public long getMaxSingleSize() {
		return maxSingleSize;
	}
	public void setMaxSingleSize(long maxSingleSize) {
		this.maxSingleSize = maxSingleSize;
	}
	
	public long getSendTimeoutMs() {
		return sendTimeoutMs;
	}
	public void setSendTimeoutMs(long sendTimeoutMs) {
		this.sendTimeoutMs = sendTimeoutMs;
	}
	public long getMaxSuspendTime() {
		return maxSuspendTime;
	}
	public void setMaxSuspendTime(long maxSuspendTime) {
		this.maxSuspendTime = maxSuspendTime;
	}
	public List<TransferTaskConvert> getConvertConfigs() {
		return convertConfigs;
	}
	public void setConvertConfigs(List<TransferTaskConvert> convertConfigs) {
		this.convertConfigs = convertConfigs;
	}
	public String getLogLevel() {
		return logLevel;
	}
	public void setLogLevel(String logLevel) {
		this.logLevel = logLevel;
	}
	@Override
	public String toString() {
		return "ProducerConfig [clientShare=" + clientShare + ", maxBatchRecords=" + maxBatchRecords + ", maxBatchSize="
				+ maxBatchSize + ", maxSingleSize=" + maxSingleSize + ", sendTimeoutMs=" + sendTimeoutMs
				+ ", maxSuspendTime=" + maxSuspendTime + ", convertConfigs=" + convertConfigs + ", clientConfig="
				+ clientConfig + "]";
	}
	
}
