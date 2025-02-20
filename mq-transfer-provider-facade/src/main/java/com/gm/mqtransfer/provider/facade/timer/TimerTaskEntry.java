package com.gm.mqtransfer.provider.facade.timer;

public class TimerTaskEntry implements Comparable<TimerTaskEntry>{
	
	private TimerTask timerTask;
	private Long expirationMs;
	
	private volatile TimerTaskList list;
	private TimerTaskEntry next;
	private TimerTaskEntry prev;
	
	public TimerTaskEntry(TimerTask timerTask, Long expirationMs) {
		this.timerTask = timerTask;
		this.expirationMs = expirationMs;
		// if this timerTask is already held by an existing timer task entry,
		// setTimerTaskEntry will remove it.
		if (timerTask != null) timerTask.setTimerTaskEntry(this);
	}
	
	public TimerTask getTimerTask() {
		return timerTask;
	}

	public void setTimerTask(TimerTask timerTask) {
		this.timerTask = timerTask;
	}

	public TimerTaskList getList() {
		return list;
	}

	public void setList(TimerTaskList list) {
		this.list = list;
	}

	public TimerTaskEntry getNext() {
		return next;
	}

	public void setNext(TimerTaskEntry next) {
		this.next = next;
	}

	public TimerTaskEntry getPrev() {
		return prev;
	}

	public void setPrev(TimerTaskEntry prev) {
		this.prev = prev;
	}

	public Long getExpirationMs() {
		return expirationMs;
	}

	public void setExpirationMs(Long expirationMs) {
		this.expirationMs = expirationMs;
	}

	public boolean cancelled() {
	    return this.timerTask.getTimerTaskEntry() != this;
	}

	public void remove() {
		TimerTaskList currentList = list;
	    // If remove is called when another thread is moving the entry from a task entry list to another,
	    // this may fail to remove the entry due to the change of value of list. Thus, we retry until the list becomes null.
	    // In a rare case, this thread sees null and exits the loop, but the other thread insert the entry to another list later.
	    while (currentList != null) {
	      currentList.remove(this);
	      currentList = list;
	    }
	}

	@Override
	public int compareTo(TimerTaskEntry o) {
		return this.expirationMs.compareTo(o.expirationMs);
	}
}
