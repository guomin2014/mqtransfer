package com.gm.mqtransfer.provider.facade.timer;

public abstract class TimerTask implements Runnable {

	/**
	 * timestamp in millisecond, Typically, it is the request.timeout.ms parameter value
	 */
	protected Long delayMs;

	private TimerTaskEntry timerTaskEntry = null;

	public synchronized void cancel() {
		if (timerTaskEntry != null)
			timerTaskEntry.remove();
		timerTaskEntry = null;
	}

	public Long getDelayMs() {
		return delayMs;
	}

	public void setDelayMs(Long delayMs) {
		this.delayMs = delayMs;
	}

	public synchronized void setTimerTaskEntry(TimerTaskEntry entry) {
		// if this timerTask is already held by an existing timer task entry,
		// we will remove such an entry first.
		if (timerTaskEntry != null && timerTaskEntry != entry)
			this.timerTaskEntry.remove();

		this.timerTaskEntry = entry;
	}

	public TimerTaskEntry getTimerTaskEntry() {
		return this.timerTaskEntry;
	}
}
