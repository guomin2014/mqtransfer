package com.gm.mqtransfer.worker.model;

/**
 * 拉取消息请求对象
 * @author GuoMin
 * @date 2023-05-19 15:15:45
 *
 */
public class PollRequest {
	/** 分区任务，当为共享线程创建请求时，该字段值为空 */
	private TaskSharding task;
	/** 资源名 */
	private String resourceName;
	/** 每行最大大小 */
	private long maxSingleSize;
	/** 每批最大大小 */
	private long maxBatchSize;
	/** 每批最大行数 */
	private int maxBatchRecords;
	/** 每批最大等待时长，单位：毫秒 */
	private long maxBatchWaitMs;
	/** 每批最大分区数 */
	private int maxBatchPartitionNums;
	/** 最多可缓存行数 */
	private int maxCacheRecords;
	/** 本次请求最大行数 */
	private int maxFetchRecords;
	/** 本次请求最大大小 */
	private long maxFetchSize;
	
	public PollRequest(TaskSharding task) {
		this.task = task;
		this.resourceName = task.getTaskShardingConfig().getTaskCode();
	}
	public TaskSharding getTask() {
		return task;
	}
	public void setTask(TaskSharding task) {
		this.task = task;
	}
	public String getResourceName() {
		return resourceName;
	}
	public void setResourceName(String resourceName) {
		this.resourceName = resourceName;
	}
	public long getMaxSingleSize() {
		return maxSingleSize;
	}
	public void setMaxSingleSize(long maxSingleSize) {
		this.maxSingleSize = maxSingleSize;
	}
	public long getMaxBatchSize() {
		return maxBatchSize;
	}
	public void setMaxBatchSize(long maxBatchSize) {
		this.maxBatchSize = maxBatchSize;
	}
	public int getMaxBatchRecords() {
		return maxBatchRecords;
	}
	public void setMaxBatchRecords(int maxBatchRecords) {
		this.maxBatchRecords = maxBatchRecords;
	}
	public long getMaxBatchWaitMs() {
		return maxBatchWaitMs;
	}
	public void setMaxBatchWaitMs(long maxBatchWaitMs) {
		this.maxBatchWaitMs = maxBatchWaitMs;
	}
	public int getMaxBatchPartitionNums() {
		return maxBatchPartitionNums;
	}
	public void setMaxBatchPartitionNums(int maxBatchPartitionNums) {
		this.maxBatchPartitionNums = maxBatchPartitionNums;
	}
	public int getMaxCacheRecords() {
		return maxCacheRecords;
	}
	public void setMaxCacheRecords(int maxCacheRecords) {
		this.maxCacheRecords = maxCacheRecords;
	}
	public int getMaxFetchRecords() {
		return maxFetchRecords;
	}
	public void setMaxFetchRecords(int maxFetchRecords) {
		this.maxFetchRecords = maxFetchRecords;
	}
	public long getMaxFetchSize() {
		return maxFetchSize;
	}
	public void setMaxFetchSize(long maxFetchSize) {
		this.maxFetchSize = maxFetchSize;
	}
	
}
