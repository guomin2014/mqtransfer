package com.gm.mqtransfer.worker.model;

/**
 * 放入缓存的响应
 * @author GuoMin
 * @date 2023-05-19 14:12:31
 *
 */
public class PutCacheResponse {
	
	/** 本次缓存消息条数 */
	private int cacheMessageCount = 0;
	/** 本次缓存消息占用内存大小 */
	private long cacheMessageSize = 0;
	/** 当前缓存队列消息条数 */
	private int cacheQueueCount;
	/** 当前缓存队列消息大小 */
	private long cacheQueueSize;
	
	public int getCacheMessageCount() {
		return cacheMessageCount;
	}
	public void setCacheMessageCount(int cacheMessageCount) {
		this.cacheMessageCount = cacheMessageCount;
	}
	public long getCacheMessageSize() {
		return cacheMessageSize;
	}
	public void setCacheMessageSize(long cacheMessageSize) {
		this.cacheMessageSize = cacheMessageSize;
	}
	public int getCacheQueueCount() {
		return cacheQueueCount;
	}
	public void setCacheQueueCount(int cacheQueueCount) {
		this.cacheQueueCount = cacheQueueCount;
	}
	public long getCacheQueueSize() {
		return cacheQueueSize;
	}
	public void setCacheQueueSize(long cacheQueueSize) {
		this.cacheQueueSize = cacheQueueSize;
	}

}
