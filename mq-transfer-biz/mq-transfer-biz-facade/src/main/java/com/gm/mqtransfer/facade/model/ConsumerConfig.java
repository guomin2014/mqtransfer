package com.gm.mqtransfer.facade.model;

import java.util.List;

/**
 * 消费者配置
 * @author GM
 * @date 2022-07-04
 */
public class ConsumerConfig {

	/** 共享消费者客户端，默认：不共享 */
	private boolean clientShare = false;
	/** 位置点提交模式，空：表示同步，async：异步，sync：同步 */
	private String commitOffsetMode = "sync";
	/** 消费组 */
	private String groupId;
	/** 消费消息批量最大可拉取记录数 */
	private int maxBatchRecords = 1000;
	/** 消费消息批量最大可拉取大小 */
	private long maxBatchSize = 25165824;//24M;
	/** 消费消息单条最大大小，单位：byte */
	private long maxSingleSize = 8388608;//8M
	/** 消费消息最大等待时间，单位：毫秒 */
	private long maxBatchWaitMs = 2000;//2s
	/** 消费消息最大暂停时间，单位：毫秒 */
	private long maxSuspendTime = 2000;//2s
	/** 消费过滤配置 */
	private List<TransferTaskFilter> filterConfigs;
	/** 原生消费客户端配置 */
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
	
	public String getCommitOffsetMode() {
		return commitOffsetMode;
	}
	public void setCommitOffsetMode(String commitOffsetMode) {
		this.commitOffsetMode = commitOffsetMode;
	}
	public String getGroupId() {
		return groupId;
	}
	public void setGroupId(String groupId) {
		this.groupId = groupId;
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
	public long getMaxBatchWaitMs() {
		return maxBatchWaitMs;
	}
	public void setMaxBatchWaitMs(long maxBatchWaitMs) {
		this.maxBatchWaitMs = maxBatchWaitMs;
	}
	public long getMaxSuspendTime() {
		return maxSuspendTime;
	}
	public void setMaxSuspendTime(long maxSuspendTime) {
		this.maxSuspendTime = maxSuspendTime;
	}
	public List<TransferTaskFilter> getFilterConfigs() {
		return filterConfigs;
	}
	public void setFilterConfigs(List<TransferTaskFilter> filterConfigs) {
		this.filterConfigs = filterConfigs;
	}
	public String getLogLevel() {
		return logLevel;
	}
	public void setLogLevel(String logLevel) {
		this.logLevel = logLevel;
	}
	@Override
	public String toString() {
		return "ConsumerConfig [clientShare=" + clientShare + ", commitOffsetMode=" + commitOffsetMode + ", groupId="
				+ groupId + ", maxBatchRecords=" + maxBatchRecords + ", maxBatchSize=" + maxBatchSize
				+ ", maxSingleSize=" + maxSingleSize + ", maxBatchWaitMs=" + maxBatchWaitMs + ", maxSuspendTime="
				+ maxSuspendTime + ", filterConfigs=" + filterConfigs + ", clientConfig=" + clientConfig + "]";
	}
	
}
