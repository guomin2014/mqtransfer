package com.gm.mqtransfer.worker.service.task.cluster.lock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 资源分区锁(分段锁)（类似行锁）
 * 
 * @author GuoMin
 * @date 2023-05-09 15:51:47
 *
 */
public class ResourcePartitionLock {
	private List<Lock> lockList;

	public ResourcePartitionLock() {
		this(10);
	}

	public ResourcePartitionLock(int size) {
		if (size < 1) {
			throw new IllegalArgumentException("size must great than 0");
		}
		this.lockList = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			this.lockList.add(new ReentrantLock());
		}
	}

	public void lock(String key) {
		int lockIndex = this.getLockIndex(key);
		this.lockList.get(lockIndex).lock();
	}

	public void unlock(String key) {
		int lockIndex = this.getLockIndex(key);
		this.lockList.get(lockIndex).unlock();
	}

	private int getLockIndex(String key) {
		int lockIndex = key.hashCode() % this.lockList.size();
		return Math.abs(lockIndex);
	}

	public boolean tryLock(String key, long time, TimeUnit timeUnit) throws InterruptedException {
		int lockIndex = this.getLockIndex(key);
		if (time < 1 || timeUnit == null) {
			return this.lockList.get(lockIndex).tryLock();
		}
		return this.lockList.get(lockIndex).tryLock(time, timeUnit);
	}

	public boolean tryLock(String key) throws InterruptedException {
		return this.tryLock(key, 0, TimeUnit.SECONDS);
	}
}
