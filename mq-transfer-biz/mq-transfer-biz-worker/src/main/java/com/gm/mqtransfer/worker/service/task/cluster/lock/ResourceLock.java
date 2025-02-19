package com.gm.mqtransfer.worker.service.task.cluster.lock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.StampedLock;

/**
 * 资源锁（类似表锁）
 * 
 * @author GuoMin
 * @date 2023-05-09 15:51:16
 *
 */
public class ResourceLock {
	// 表锁
	private final StampedLock STAMPED_LOCK = new StampedLock();
	// 行锁
	private ResourcePartitionLock rowLocks;

	public ResourceLock() {
		this(10);
	}

	public ResourceLock(int rows) {
		rowLocks = new ResourcePartitionLock(rows);
	}

	/***
	 * @description:释放表锁
	 * @param time*
	 * @param timeUnit
	 * @return long 邮戳，为0表示加锁失败
	 */
	public long tryLockTable(int time, TimeUnit timeUnit) throws InterruptedException {
		return STAMPED_LOCK.tryWriteLock(time, timeUnit);
	}

	public long tryLockTable() throws InterruptedException {
		return STAMPED_LOCK.tryWriteLock(0, TimeUnit.SECONDS);
	}

	/***
	 * @description:释放表锁
	 * @param stamp 获取表锁返回的邮戳
	 * @see #tryLockTable(int, TimeUnit)
	 * @return void
	 */
	public void unLockTable(long stamp) {
		STAMPED_LOCK.unlockWrite(stamp);
	}

	/***
	 * @description:加行锁
	 * @param key
	 * @param time
	 * @param timeUnit
	 * @return long	邮戳，为0表示加锁失败
	 */
	public long tryLockRow(String key, int time, TimeUnit timeUnit) throws InterruptedException {
		// 先设置表锁为已读状态
		long stamp = STAMPED_LOCK.tryReadLock(time, timeUnit);
		// 失败，直接返回
		if (stamp == 0)
			return stamp;
		// 锁行
		boolean lockedRow = rowLocks.tryLock(key, time, timeUnit);
		if (!lockedRow) {
			STAMPED_LOCK.unlockRead(stamp);
			return 0;
		}
		return stamp;
	}

	public long tryLockRow(String key) throws InterruptedException {
		return tryLockRow(key, 0, TimeUnit.SECONDS);
	}

	/***
	 * @description:释放行锁
	 * @param key
	 * @param stamp 获取行锁获得的邮戳
	 * @see #tryLockRow(String, int, TimeUnit)
	 */
	public void unlockRow(String key, long stamp) {
		STAMPED_LOCK.unlockRead(stamp);
		rowLocks.unlock(key);
	}
}
