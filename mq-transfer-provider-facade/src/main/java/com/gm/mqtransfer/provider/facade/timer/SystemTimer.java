package com.gm.mqtransfer.provider.facade.timer;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SystemTimer implements Timer {

//	private String executorName;
	private Long tickMs = 1L;
	private int wheelSize = 20;
	private Long startMs = Time.SYSTEM.hiResClockMs();

	// timeout timer
	private ExecutorService taskExecutor = null;

	private DelayQueue<TimerTaskList> delayQueue = new DelayQueue<>();
	private AtomicInteger taskCounter = new AtomicInteger(0);
	private TimingWheel timingWheel;

	// Locks used to protect data structures while ticking
	private ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
	private ReentrantReadWriteLock.ReadLock readLock = readWriteLock.readLock();
	private ReentrantReadWriteLock.WriteLock writeLock = readWriteLock.writeLock();

	public SystemTimer(String executorName) {
		this(executorName, 1L, 20, Time.SYSTEM.hiResClockMs());
	}
	public SystemTimer(String executorName, Long tickMs, Integer wheelSize, Long startMs) {
//		this.executorName = executorName;
		if (tickMs != null) {
			this.tickMs = tickMs;
		}
		if (wheelSize != null) {
			this.wheelSize = wheelSize.intValue();
		}
		if (startMs != null) {
			this.startMs = startMs;
		}
		taskExecutor = Executors.newFixedThreadPool(1, new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				return new Thread(r, "executor-" + executorName);
			}
		});
		timingWheel = new TimingWheel(this.tickMs, this.wheelSize, this.startMs, taskCounter, delayQueue);
	}

	@Override
	public void add(TimerTask timerTask) {
		readLock.lock();
		try {
			addTimerTaskEntry(new TimerTaskEntry(timerTask, timerTask.getDelayMs() != null ? timerTask.getDelayMs() + Time.SYSTEM.hiResClockMs() : Time.SYSTEM.hiResClockMs()));
		} finally {
			readLock.unlock();
		}
	}

	/*
	 * Advances the clock if there is an expired bucket. If there isn't any expired
	 * bucket when called, waits up to timeoutMs before giving up.
	 */
	@Override
	public boolean advanceClock(Long timeoutMs) {
		TimerTaskList bucket = null;
		try {
			bucket = delayQueue.poll(timeoutMs, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (bucket != null) {
			writeLock.lock();
			try {
				while (bucket != null) {
					timingWheel.advanceClock(bucket.getExpiration().longValue());
					bucket.flush(t -> {reinsert(t);});
					bucket = delayQueue.poll();
				}
			} finally {
				writeLock.unlock();
			}
			return true;
		} else {
			return false;
		}
	}

	@Override
	public int size() {
		return taskCounter.get();
	}

	@Override
	public void shutdown() {
		taskExecutor.shutdown();
	}

	private void addTimerTaskEntry(TimerTaskEntry timerTaskEntry) {
		if (!timingWheel.add(timerTaskEntry)) {
			// Already expired or cancelled
			if (!timerTaskEntry.cancelled())
				taskExecutor.submit(timerTaskEntry.getTimerTask());
		}
	}

	private void reinsert(TimerTaskEntry timerTaskEntry) {
		addTimerTaskEntry(timerTaskEntry);
	}

}
