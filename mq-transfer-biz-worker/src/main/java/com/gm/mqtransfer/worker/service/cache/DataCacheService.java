package com.gm.mqtransfer.worker.service.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.stereotype.Component;

import com.gm.mqtransfer.facade.model.CustomMessage;
import com.gm.mqtransfer.worker.model.CacheBatch;
import com.gm.mqtransfer.worker.model.TaskSharding;

@Component
public class DataCacheService {
	
	private Map<TaskSharding, CacheBatch> taskCacheMessageMap = new ConcurrentHashMap<>();
	
	private final ReentrantLock lock = new ReentrantLock();
	
	private CacheBatch getCacheBatch(TaskSharding task) {
		CacheBatch batch = taskCacheMessageMap.get(task);
		if (batch == null) {
			lock.lock();
			try {
				batch = taskCacheMessageMap.get(task);
				if (batch == null) {
					batch = new CacheBatch();
					taskCacheMessageMap.put(task, batch);
				}
			} finally {
				lock.unlock();
			}
		}
		return batch;
	}
	/**
	 * 加入缓存队列并返回当前队列数量
	 * @param resourceName
	 * @param topic
	 * @param queueId
	 * @param messageList
	 */
	public CacheBatch putAndGet(TaskSharding task, List<CustomMessage> messageList) {
		CacheBatch batch = this.getCacheBatch(task);
		batch.put(messageList);
		return batch;
	}
	/**
	 * 将消息重新发入队列头中
	 * @param resourceName
	 * @param topic
	 * @param queueId
	 * @param messageList
	 */
	public CacheBatch putHead(TaskSharding task, List<CustomMessage> messageList) {
		CacheBatch batch = this.getCacheBatch(task);
		batch.putHead(messageList);
		return batch;
	}
	public void putHead(TaskSharding task, CustomMessage message) {
		List<CustomMessage> messageList = new ArrayList<>();
		messageList.add(message);
		this.putHead(task, messageList);
	}
	/**
	 * 获取队首指定数量元素并移除
	 * @param resourceName
	 * @param topic
	 * @param queueId
	 * @param maxSize
	 * @return
	 */
	public List<CustomMessage> poll(TaskSharding task, int maxSize) {
		CacheBatch batch = this.getCacheBatch(task);
		return batch.poll(maxSize);
	}
	public CustomMessage poll(TaskSharding task) {
		CacheBatch batch = this.getCacheBatch(task);
		return batch.poll();
	}
	/**
	 * 从缓存队列清除
	 * @param resourceName
	 * @param topic
	 * @param queueId
	 * @return
	 */
	public CacheBatch cleanQueue(TaskSharding task) {
		lock.lock();
		try {
			return taskCacheMessageMap.remove(task);
		} finally {
			lock.unlock();
		}
	}
	/**
	 * 统计缓存队列数
	 * @param resourceName
	 * @return
	 */
	public int countQueue(String taskCode) {
		int totalSize = 0;
		for (TaskSharding task : taskCacheMessageMap.keySet()) {
			if (task.getTaskShardingConfig().getTaskCode().equalsIgnoreCase(taskCode)) {
				CacheBatch batch = taskCacheMessageMap.get(task);
				if (batch != null) {
					totalSize += batch.getMessageCount();
				}
			}
		}
		return totalSize;
	}
	/**
	 * 获取缓存队列大小
	 * @param task
	 * @return
	 */
	public int getQueueCount(TaskSharding task) {
		CacheBatch batch = this.getCacheBatch(task);
		return batch.getMessageCount();
	}
	/**
	 * 统计分区缓存大小（字节）
	 * @param resourceName
	 * @param brokerName
	 * @param topic
	 * @param queueId
	 * @return
	 */
	public long countQueueSize(TaskSharding task) {
		CacheBatch batch = this.getCacheBatch(task);
		return batch.getMessageSize();
	}
}
