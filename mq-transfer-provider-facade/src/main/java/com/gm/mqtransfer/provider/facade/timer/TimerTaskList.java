package com.gm.mqtransfer.provider.facade.timer;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class TimerTaskList implements Delayed {

	private AtomicInteger taskCounter;

	private TimerTaskEntry root = null;

	private AtomicLong expiration = new AtomicLong(-1L);

	public TimerTaskList(AtomicInteger taskCounter) {
		this.taskCounter = taskCounter;
		// TimerTaskList forms a doubly linked cyclic list using a dummy root entry
		// root.next points to the head
		// root.prev points to the tail
		root = new TimerTaskEntry(null, -1L);
		root.setNext(root);
		root.setPrev(root);
	}

	public boolean setExpiration(Long expirationMs) {
		return expiration.getAndSet(expirationMs) != expirationMs;
	}

	// Get the bucket's expiration time
	public Long getExpiration() {
		return expiration.get();
	}

	@Override
	public int compareTo(Delayed o) {
		TimerTaskList other = (TimerTaskList) o;
		if (getExpiration() < other.getExpiration())
			return -1;
		else if (getExpiration() > other.getExpiration())
			return 1;
		else
			return 0;
	}

	@Override
	public long getDelay(TimeUnit unit) {
		long time = Math.max(getExpiration().longValue() - Time.SYSTEM.hiResClockMs(), 0);
		return unit.convert(time, TimeUnit.MILLISECONDS);
	}

	// Apply the supplied function to each of tasks in this list
	public synchronized void foreach(Consumer<TimerTask> f) {
		TimerTaskEntry entry = root.getNext();
		while (entry != root) {
			TimerTaskEntry nextEntry = entry.getNext();
			if (!entry.cancelled()) {
				f.accept(entry.getTimerTask());
			}
			entry = nextEntry;
		}
	}

	// Add a timer task entry to this list
	public synchronized void add(TimerTaskEntry timerTaskEntry) {
		boolean done = false;
		while (!done) {
			// Remove the timer task entry if it is already in any other list
			// We do this outside of the sync block below to avoid deadlocking.
			// We may retry until timerTaskEntry.list becomes null.
			timerTaskEntry.remove();
			if (timerTaskEntry.getList() == null) {
				// put the timer task entry to the end of the list. (root.prev points to the
				// tail entry)
				TimerTaskEntry tail = root.getPrev();
				timerTaskEntry.setNext(root);
				timerTaskEntry.setPrev(tail);
				timerTaskEntry.setList(this);
				tail.setNext(timerTaskEntry);
				root.setPrev(timerTaskEntry);
				taskCounter.incrementAndGet();
				done = true;
			}
		}
	}

	// Remove the specified timer task entry from this list
	public synchronized void remove(TimerTaskEntry timerTaskEntry) {
		if (timerTaskEntry.getList() == this) {
			timerTaskEntry.getNext().setPrev(timerTaskEntry.getPrev());
			timerTaskEntry.getPrev().setNext(timerTaskEntry.getNext());
			timerTaskEntry.setNext(null);
			timerTaskEntry.setPrev(null);
			timerTaskEntry.setList(null);
			taskCounter.decrementAndGet();
		}
	}

	// Remove all task entries and apply the supplied function to each of them
	public synchronized void flush(Consumer<TimerTaskEntry> f) {
		TimerTaskEntry head = root.getNext();
		while (head != root) {
			remove(head);
			f.accept(head);
			head = root.getNext();
		}
		expiration.set(-1L);
	}

}
