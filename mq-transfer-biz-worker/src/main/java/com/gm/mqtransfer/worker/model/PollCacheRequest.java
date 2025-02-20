package com.gm.mqtransfer.worker.model;

/**
 * 从缓存拉取消息请求对象
 * @author GuoMin
 * @date 2023-05-19 15:15:45
 *
 */
public class PollCacheRequest {
	/** 分区任务 */
	private TaskSharding task;
	/** 资源名 */
	private String resourceName;
	/** 每行最大大小 */
	private long maxSingleSize;
	/** 每批最大大小 */
	private long maxBatchSize;
	/** 每批最大行数 */
	private int maxBatchRecords;
	/** 最大转发次数 */
	private int maxTTL;
	
	public PollCacheRequest(TaskSharding task) {
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
	public int getMaxTTL() {
		return maxTTL;
	}
	public void setMaxTTL(int maxTTL) {
		this.maxTTL = maxTTL;
	}
	
}
