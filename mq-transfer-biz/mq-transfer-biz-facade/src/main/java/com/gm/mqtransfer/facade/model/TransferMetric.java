package com.gm.mqtransfer.facade.model;

import java.io.Serializable;

public class TransferMetric implements Serializable{

	private static final long serialVersionUID = -3144522931604851290L;

	private long timestamp;
	// 任务执行集群
	private String cluster;
	// 任务执行实例
	private String instance;
    // 任务编号
    private String taskId;

    private String sourceCluster;

    private String sourceTopic;
    // 分区
    private String sourcePartition;

    private String targetCluster;

    private String targetTopic;
    // 分区
    private String targetPartition;
    // 消费下来的消息条数
    private Long consumeNumber;
    // 消费下来的字节数
    private Long consumeBytes;
    // 已转发的消息条数
    private Long sentNumber;
    // 已转发的字节数
    private Long sentBytes;
    // topic中未消费的消息数
    private Long sourceSurplusNumber;
    // 被过滤的消息数
    private Long filterNumber;
    // 被过滤的消息大小
    private Long filterBytes;
    // 缓存的消息数
    private Long cacheNumber;
    // 缓存的消息大小
    private Long cacheBytes;
    
    public TransferMetric(String cluster, String instance, String taskId, String sourceCluster, String sourceTopic, String sourcePartition, 
    		String targetCluster, String targetTopic, String targetPartition, 
    		Long sourceSurplusNumber, Long consumeNumber, Long consumeBytes, Long filterNumber, Long filterBytes,
    		Long cacheNumber, Long cacheBytes) {
    	this.timestamp = System.currentTimeMillis();
    	this.cluster = cluster;
    	this.instance = instance;
    	this.taskId = taskId;
    	this.sourceCluster = sourceCluster;
    	this.sourceTopic = sourceTopic;
    	this.sourcePartition = sourcePartition;
    	this.targetCluster = targetCluster;
    	this.targetTopic = targetTopic;
    	this.targetPartition = targetPartition;
    	this.consumeNumber = consumeNumber;
    	this.consumeBytes = consumeBytes;
    	this.filterNumber = filterNumber;
    	this.sourceSurplusNumber = sourceSurplusNumber;
    	this.filterBytes = filterBytes;
    	this.cacheNumber = cacheNumber;
    	this.cacheBytes = cacheBytes;
    }
    public TransferMetric(String cluster, String instance, String taskId, String sourceCluster, String sourceTopic, String sourcePartition, 
    		String targetCluster, String targetTopic, String targetPartition, 
    		Long sentNumber, Long sentBytes, Long filterNumber, Long filterBytes) {
    	this.timestamp = System.currentTimeMillis();
    	this.cluster = cluster;
    	this.instance = instance;
    	this.taskId = taskId;
    	this.sourceCluster = sourceCluster;
    	this.sourceTopic = sourceTopic;
    	this.sourcePartition = sourcePartition;
    	this.targetCluster = targetCluster;
    	this.targetTopic = targetTopic;
    	this.targetPartition = targetPartition;
    	this.sentNumber = sentNumber;
    	this.sentBytes = sentBytes;
    }
	public long getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	public String getTaskId() {
		return taskId;
	}
	public void setTaskId(String taskId) {
		this.taskId = taskId;
	}
	public String getSourceCluster() {
		return sourceCluster;
	}
	public void setSourceCluster(String sourceCluster) {
		this.sourceCluster = sourceCluster;
	}
	public String getSourceTopic() {
		return sourceTopic;
	}
	public void setSourceTopic(String sourceTopic) {
		this.sourceTopic = sourceTopic;
	}
	public String getSourcePartition() {
		return sourcePartition;
	}
	public void setSourcePartition(String sourcePartition) {
		this.sourcePartition = sourcePartition;
	}
	public String getTargetCluster() {
		return targetCluster;
	}
	public void setTargetCluster(String targetCluster) {
		this.targetCluster = targetCluster;
	}
	public String getTargetTopic() {
		return targetTopic;
	}
	public void setTargetTopic(String targetTopic) {
		this.targetTopic = targetTopic;
	}
	public String getTargetPartition() {
		return targetPartition;
	}
	public void setTargetPartition(String targetPartition) {
		this.targetPartition = targetPartition;
	}
	public Long getConsumeNumber() {
		return consumeNumber;
	}
	public void setConsumeNumber(Long consumeNumber) {
		this.consumeNumber = consumeNumber;
	}
	public Long getConsumeBytes() {
		return consumeBytes;
	}
	public void setConsumeBytes(Long consumeBytes) {
		this.consumeBytes = consumeBytes;
	}
	public Long getSentNumber() {
		return sentNumber;
	}
	public void setSentNumber(Long sentNumber) {
		this.sentNumber = sentNumber;
	}
	public Long getSentBytes() {
		return sentBytes;
	}
	public void setSentBytes(Long sentBytes) {
		this.sentBytes = sentBytes;
	}
	public Long getSourceSurplusNumber() {
		return sourceSurplusNumber;
	}
	public void setSourceSurplusNumber(Long sourceSurplusNumber) {
		this.sourceSurplusNumber = sourceSurplusNumber;
	}
	public Long getFilterNumber() {
		return filterNumber;
	}
	public void setFilterNumber(Long filterNumber) {
		this.filterNumber = filterNumber;
	}
	public Long getFilterBytes() {
		return filterBytes;
	}
	public void setFilterBytes(Long filterBytes) {
		this.filterBytes = filterBytes;
	}
	public Long getCacheNumber() {
		return cacheNumber;
	}
	public void setCacheNumber(Long cacheNumber) {
		this.cacheNumber = cacheNumber;
	}
	public Long getCacheBytes() {
		return cacheBytes;
	}
	public void setCacheBytes(Long cacheBytes) {
		this.cacheBytes = cacheBytes;
	}
	public String getCluster() {
		return cluster;
	}
	public void setCluster(String cluster) {
		this.cluster = cluster;
	}
	public String getInstance() {
		return instance;
	}
	public void setInstance(String instance) {
		this.instance = instance;
	}
	@Override
	public String toString() {
		return "ForwardMetric [timestamp=" + timestamp + ", cluster=" + cluster + ", instance=" + instance + ", taskId="
				+ taskId + ", sourceCluster=" + sourceCluster + ", sourceTopic=" + sourceTopic + ", sourcePartition="
				+ sourcePartition + ", targetCluster=" + targetCluster + ", targetTopic=" + targetTopic
				+ ", targetPartition=" + targetPartition + ", consumeNumber=" + consumeNumber + ", consumeBytes="
				+ consumeBytes + ", sentNumber=" + sentNumber + ", sentBytes=" + sentBytes + ", sourceSurplusNumber="
				+ sourceSurplusNumber + ", filterNumber=" + filterNumber + ", filterBytes=" + filterBytes
				+ ", cacheNumber=" + cacheNumber + ", cacheBytes=" + cacheBytes + "]";
	}
    
}
