package com.gm.mqtransfer.provider.facade.service.delay;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gm.mqtransfer.provider.facade.timer.SystemTimer;
import com.gm.mqtransfer.provider.facade.timer.Timer;

public class DelayedOperationPurgatory<T extends DelayedOperation> {

//	private int brokerId = 0;
	private int purgeInterval = 1000;
	private boolean reaperEnabled = true;
	private boolean timerEnabled = true;
	private Timer timeoutTimer;

	private int shards = 512; // Shard the watcher list to reduce lock contention

	private List<WatcherList> watcherLists = new ArrayList<>(shards);

	// the number of estimated total operations in the purgatory
	private AtomicInteger estimatedTotalOperations = new AtomicInteger(0);

	/* background thread expiring operations that have timed out */
	private ExpiredOperationReaper expirationReaper = null;
	
	private Logger logger = LoggerFactory.getLogger(DelayedOperationPurgatory.class);

	public DelayedOperationPurgatory(String purgatoryName) {
		timeoutTimer = new SystemTimer(purgatoryName);
		if (reaperEnabled) {
			expirationReaper = new ExpiredOperationReaper(purgatoryName);
			expirationReaper.start();
		}
		for (int i = 0; i < shards; i++) {
			watcherLists.add(new WatcherList());
		}
	}

	private WatcherList watcherList(Object key) {
		int index = Math.abs(key.hashCode() % watcherLists.size());
		return watcherLists.get(index);
	}

	/**
	 * Check if the operation can be completed, if not watch it based on the given
	 * watch keys
	 *
	 * Note that a delayed operation can be watched on multiple keys. It is possible
	 * that an operation is completed after it has been added to the watch list for
	 * some, but not all of the keys. In this case, the operation is considered
	 * completed and won't be added to the watch list of the remaining keys. The
	 * expiration reaper thread will remove this operation from any watcher list in
	 * which the operation exists.
	 *
	 * @param operation the delayed operation to be checked
	 * @param watchKeys keys for bookkeeping the operation
	 * @return true iff the delayed operations can be completed by the caller
	 */
	public boolean tryCompleteElseWatch(T operation, Queue<Object> watchKeys) {
		if (watchKeys.isEmpty()) {
			throw new IllegalArgumentException("The watch key list can't be empty");
		}

		// The cost of tryComplete() is typically proportional to the number of keys.
		// Calling
		// tryComplete() for each key is going to be expensive if there are many keys.
		// Instead,
		// we do the check in the following way. Call tryComplete(). If the operation is
		// not completed,
		// we just add the operation to all keys. Then we call tryComplete() again. At
		// this time, if
		// the operation is still not completed, we are guaranteed that it won't miss
		// any future triggering
		// event since the operation is already on the watcher list for all keys. This
		// does mean that
		// if the operation is completed (by another thread) between the two
		// tryComplete() calls, the
		// operation is unnecessarily added for watch. However, this is a less severe
		// issue since the
		// expire reaper will clean it up periodically.

		// At this point the only thread that can attempt this operation is this current
		// thread
		// Hence it is safe to tryComplete() without a lock
		boolean isCompletedByMe = operation.tryComplete();
		if (isCompletedByMe)
			return true;

		boolean watchCreated = false;
		for (Object key : watchKeys) {
			// If the operation is already completed, stop adding it to the rest of the
			// watcher list.
			if (operation.isCompleted())
				return false;
			watchForOperation(key, operation);

			if (!watchCreated) {
				watchCreated = true;
				estimatedTotalOperations.incrementAndGet();
			}
		}

		isCompletedByMe = operation.maybeTryComplete();
		if (isCompletedByMe)
			return true;

		// if it cannot be completed by now and hence is watched, add to the expire
		// queue also
		if (!operation.isCompleted()) {
			if (timerEnabled) {
				timeoutTimer.add(operation);
//				logger.info("add timer [delayMs:{}] --> {}", operation.delayMs, watchKeys);
			}
			if (operation.isCompleted()) {
				// cancel the timer task
				operation.cancel();
			}
		}

		return false;
	}

	/**
	 * Check if some delayed operations can be completed with the given watch key,
	 * and if yes complete them.
	 *
	 * @return the number of completed operations during this process
	 */
	public int checkAndComplete(Object key) {
		WatcherList wl = watcherList(key);
		Watchers watchers = null;
		wl.watchersLock.lock();
		try {
			watchers = wl.watchersByKey.get(key);
		} finally {
			wl.watchersLock.unlock();
		}
		if (watchers == null)
			return 0;
		else
			return watchers.tryCompleteWatched();
	}

	/**
	 * Return the total size of watch lists the purgatory. Since an operation may be
	 * watched on multiple lists, and some of its watched entries may still be in
	 * the watch lists even when it has been completed, this number may be larger
	 * than the number of real operations watched
	 */
	public int watched() {
//	    watcherLists.foldLeft(0) { case (sum, watcherList) => sum + watcherList.allWatchers.map(_.countWatched).sum }
		int totalSize = 0;
		for (WatcherList wl : watcherLists) {
			totalSize += wl.allWatchers().size();
		}
		return totalSize;
	}

	/**
	 * Return the number of delayed operations in the expiry queue
	 */
	public int delayed() {
		return timeoutTimer.size();
	}

	/**
	 * Cancel watching on any delayed operations for the given key. Note the
	 * operation will not be completed
	 */
	public List<T> cancelForKey(Object key) {
		WatcherList wl = watcherList(key);
		wl.watchersLock.lock();
		try {
			Watchers watchers = wl.watchersByKey.remove(key);
			if (watchers != null)
				return watchers.cancel();
			else
				return null;
		} finally {
			wl.watchersLock.unlock();
		}
	}

	/*
	 * Return the watch list of the given key, note that we need to grab the
	 * removeWatchersLock to avoid the operation being added to a removed watcher
	 * list
	 */
	private void watchForOperation(Object key, T operation) {
		WatcherList wl = watcherList(key);
		wl.watchersLock.lock();
		try {
			Watchers watcher = wl.watchersByKey.get(key);
			if (watcher == null) {
				watcher = new Watchers(key);
				wl.watchersByKey.put(key, watcher);
			}
			watcher.watch(operation);
		} finally {
			wl.watchersLock.unlock();
		}
	}

	/*
	 * Remove the key from watcher lists if its list is empty
	 */
	private void removeKeyIfEmpty(Object key, Watchers watchers) {
		WatcherList wl = watcherList(key);
		wl.watchersLock.lock();
		try {
			// if the current key is no longer correlated to the watchers to remove, skip
			if (wl.watchersByKey.get(key) != watchers)
				return;

			if (watchers != null && watchers.isEmpty()) {
				wl.watchersByKey.remove(key);
			}
		} finally {
			wl.watchersLock.unlock();
		}
	}

	/**
	 * Shutdown the expire reaper thread
	 */
	public void shutdown() {
		if (reaperEnabled)
			expirationReaper.shutdown();
		timeoutTimer.shutdown();
	}

	public void advanceClock(Long timeoutMs) {
//		logger.info("advanceClock---->" + timeoutMs);
		timeoutTimer.advanceClock(timeoutMs);

		// Trigger a purge if the number of completed but still being watched operations
		// is larger than
		// the purge threshold. That number is computed by the difference btw the
		// estimated total number of
		// operations and the number of pending delayed operations.
		if (estimatedTotalOperations.get() - delayed() > purgeInterval) {
			// now set estimatedTotalOperations to delayed (the number of pending
			// operations) since we are going to
			// clean up watchers. Note that, if more operations are completed during the
			// clean up, we may end up with
			// a little overestimated total number of operations.
			estimatedTotalOperations.getAndSet(delayed());
			logger.info("Begin purging watch lists");
//	      int purged = watcherLists.foldLeft(0) {
//	        case (sum, watcherList) => sum + watcherList.allWatchers.map(_.purgeCompleted()).sum
//	      }
			int purged = this.watched();
			logger.info("Purged {} elements from watch lists.", purged);
		}
	}

	public class WatcherList {
		public Map<Object, Watchers> watchersByKey = new ConcurrentHashMap<>();

		public ReentrantLock watchersLock = new ReentrantLock();

		/*
		 * Return all the current watcher lists, note that the returned watchers may be
		 * removed from the list by other threads
		 */
		public Collection<Watchers> allWatchers() {
			return watchersByKey.values();
		}
	}

	/**
	 * A linked list of watched delayed operations based on some key
	 */
	public class Watchers {

		private Object key;

		public Watchers(Object key) {
			this.key = key;
		}

		private Queue<T> operations = new ConcurrentLinkedQueue<>();

		// count the current number of watched operations. This is O(n), so use
		// isEmpty() if possible
		public int countWatched() {
			return operations.size();
		}

		public boolean isEmpty() {
			return operations.isEmpty();
		}

		// add the element to watch
		public void watch(T t) {
			operations.add(t);
		}

		// traverse the list and try to complete some watched elements
		public int tryCompleteWatched() {
			int completed = 0;

			Iterator<T> iter = operations.iterator();
			while (iter.hasNext()) {
				T curr = iter.next();
				if (curr.isCompleted()) {
					// another thread has completed this operation, just remove it
					iter.remove();
				} else if (curr.maybeTryComplete()) {
					iter.remove();
					completed += 1;
				}
			}

			if (operations.isEmpty())
				removeKeyIfEmpty(key, this);

			return completed;
		}

		public List<T> cancel() {
			Iterator<T> iter = operations.iterator();
			List<T> cancelled = new ArrayList<>();
			while (iter.hasNext()) {
				T curr = iter.next();
				curr.cancel();
				iter.remove();
				cancelled.add(curr);
			}
			return cancelled;
		}

		// traverse the list and purge elements that are already completed by others
		public int purgeCompleted() {
			int purged = 0;

			Iterator<T> iter = operations.iterator();
			while (iter.hasNext()) {
				T curr = iter.next();
				if (curr.isCompleted()) {
					iter.remove();
					purged += 1;
				}
			}

			if (operations.isEmpty())
				removeKeyIfEmpty(key, this);

			return purged;
		}
	}

	/**
	 * A background reaper to expire delayed operations that have timed out
	 */
	private class ExpiredOperationReaper extends Thread {

		private CountDownLatch shutdownInitiated = new CountDownLatch(1);
		private CountDownLatch shutdownComplete = new CountDownLatch(1);
		private volatile boolean isStarted = false;
		private boolean isInterruptible = true;

		public ExpiredOperationReaper(String purgatoryName) {
			this.setName("ExpirationReaper-" + purgatoryName);
			this.setDaemon(false);
		}

		public boolean isRunning() {
			return shutdownInitiated.getCount() != 0;
		}

		public void run() {
			isStarted = true;
			logger.info("Starting");
			try {
				while (isRunning()) {
					advanceClock(200L);
				}
			} catch (Exception e) {
				shutdownInitiated.countDown();
				shutdownComplete.countDown();
				logger.info("Stopped");
//				        Exit.exit(e.statusCode())
			} catch (Throwable e) {
				if (isRunning()) {
					logger.error("Error due to", e);
				}
			} finally {
				shutdownComplete.countDown();
			}
			logger.info("Stopped");
		}

		public void shutdown() {
			initiateShutdown();
			awaitShutdown();
		}

		public synchronized boolean initiateShutdown() {
			if (isRunning()) {
				logger.info("Shutting down");
				shutdownInitiated.countDown();
				if (isInterruptible)
					interrupt();
				return true;
			} else
				return false;
		}

		/**
		 * After calling initiateShutdown(), use this API to wait until the shutdown is
		 * complete
		 */
		public void awaitShutdown() {
			if (shutdownInitiated.getCount() != 0)
				throw new IllegalStateException("initiateShutdown() was not called before awaitShutdown()");
			else {
				if (isStarted) {
					try {
						shutdownComplete.await();
					} catch (Exception e) {
						throw new IllegalStateException(e);
					}
				}
				logger.info("Shutdown completed");
			}
		}

//		/**
//		 * Causes the current thread to wait until the shutdown is initiated, or the
//		 * specified waiting time elapses.
//		 *
//		 * @param timeout
//		 * @param unit
//		 */
//		public void pause(Long timeout, TimeUnit unit) {
//			try {
//				if (shutdownInitiated.await(timeout, unit)) {
//	//		      trace("shutdownInitiated latch count reached zero. Shutdown called.")
//				}
//			} catch (InterruptedException e) {
//				
//			}
//		}
	}
}
