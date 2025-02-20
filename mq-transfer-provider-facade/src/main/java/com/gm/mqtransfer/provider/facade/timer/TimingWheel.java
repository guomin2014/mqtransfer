package com.gm.mqtransfer.provider.facade.timer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class TimingWheel {

	/** 滴答一次的时长，类似于手表的例子中向前推进一格的时间 */
	private long tickMs;
	/** 每一层时间轮上的Bucket数量 */
	private int wheelSize;
	/** 时间轮对象被创建时的起始时间戳 */
	private long startMs;
	/** 这一层时间轮上的总定时任务数 */
	private AtomicInteger taskCounter;
	/** 将所有Bucket按照过期时间排序的延迟队列 */
	private DelayQueue<TimerTaskList> queue;
	/** 这层时间轮总时长，等于滴答时长乘以wheelSize */
	private long interval;
	/** 时间轮下的所有Bucket对象，也就是所有TimerTaskList对象 */
	private List<TimerTaskList> buckets;
	/**
	 * 当前时间戳，只是源码对它进行了一些微调整，将它设置成小于当前时间的最大滴答时长的整数倍。
	 * 举个例子，假设滴答时长是20毫秒，当前时间戳是123毫秒，那么，currentTime会被调整为120毫秒
	 */
	private long currentTime;
	/**
	 * 按需创建上层时间轮的。这也就是说，当有新的定时任务到达时，会尝试将其放入第1层时间轮。
	 * 如果第1层的interval无法容纳定时任务的超时时间，就现场创建并配置好第2层时间轮，并再次尝试放入，
	 * 如果依然无法容纳，那么，就再创建和配置第3层时间轮，以此类推，直到找到适合容纳该定时任务的第N层时间轮。
	 */
	private TimingWheel overflowWheel;

	public TimingWheel(long tickMs, int wheelSize, long startMs, AtomicInteger taskCounter,
			DelayQueue<TimerTaskList> queue) {
		this.tickMs = tickMs;
		this.wheelSize = wheelSize;
		this.startMs = startMs;
		this.taskCounter = taskCounter;
		this.queue = queue;
		this.interval = tickMs * wheelSize;
		this.buckets = new ArrayList<>(wheelSize);
		for (int i = 0; i < wheelSize; i++) {
			this.buckets.add(new TimerTaskList(taskCounter));
		}
		this.currentTime = startMs - (startMs % tickMs);
	}

	public synchronized void addOverflowWheel() {
		if (this.overflowWheel == null) {
			this.overflowWheel = new TimingWheel(interval, wheelSize, currentTime, taskCounter, queue);
		}
	}

	public boolean add(TimerTaskEntry timerTaskEntry) {
		long expiration = timerTaskEntry.getExpirationMs();

		if (timerTaskEntry.cancelled()) {
			// Cancelled
			return false;
		} else if (expiration < currentTime + tickMs) {
			// Already expired
			return false;
		} else if (expiration < currentTime + interval) {
			// Put in its own bucket
			long virtualId = expiration / tickMs;
			TimerTaskList bucket = buckets.get((int) (virtualId % wheelSize));
			bucket.add(timerTaskEntry);

			// Set the bucket expiration time
			if (bucket.setExpiration(virtualId * tickMs)) {
				// The bucket needs to be enqueued because it was an expired bucket
				// We only need to enqueue the bucket when its expiration time has changed, i.e.
				// the wheel has advanced
				// and the previous buckets gets reused; further calls to set the expiration
				// within the same wheel cycle
				// will pass in the same value and hence return false, thus the bucket with the
				// same expiration will not
				// be enqueued multiple times.
				queue.offer(bucket);
			}
			return true;
		} else {
			// Out of the interval. Put it into the parent timer
			if (overflowWheel == null)
				addOverflowWheel();
			return overflowWheel.add(timerTaskEntry);
		}
	}

	// Try to advance the clock
	public void advanceClock(long timeMs) {
		if (timeMs >= currentTime + tickMs) {
			currentTime = timeMs - (timeMs % tickMs);

			// Try to advance the clock of the overflow wheel if present
			if (overflowWheel != null)
				overflowWheel.advanceClock(currentTime);
		}
	}
}
