/**
 * Copyright (C) 2015-2016 Uber Technology Inc. (streaming-core@uber.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gm.mqtransfer.manager.support.helix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 *TopicObserver provides topic information on this broker
 * such as all topic names and partitions for each topic
 */
public abstract class TopicObserver<T> {

	protected static final Logger LOGGER = LoggerFactory.getLogger(TopicObserver.class);

	private ScheduledExecutorService executorService = null;

	protected final AtomicLong _lastRefreshTime = new AtomicLong(0);

	private final long _refreshTimeIntervalInMillis = 2 * 60 * 1000;

	private final Object _lock = new Object();

	/**
	 * 自动发现topic,并缓存;
	 */
	protected volatile boolean autoRefreshTopic;

	public TopicObserver(final boolean autoRefreshTopic) {
		this.autoRefreshTopic = autoRefreshTopic;
	}

	public void start() {
		if (autoRefreshTopic) {
			executorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
				@Override
				public Thread newThread(Runnable r) {
					return new Thread(r, "Topic-Observer-Thread");
				}
			});
			executorService.scheduleAtFixedRate(new Runnable() {
				@Override
				public void run() {
					tryToRefreshCache();
				}
			}, 0, 60, TimeUnit.SECONDS);
		}
	}

	private synchronized void refreshCache() {
		synchronized (_lock) {
			doRefreshCache();
		}
	}

	protected abstract void doRefreshCache();

	protected synchronized boolean tryToRefreshCache() {
		if (_refreshTimeIntervalInMillis + _lastRefreshTime.get() < System.currentTimeMillis()) {
			refreshCache();
			return true;
		} else {
			LOGGER.debug("Not hitting next refresh interval, wait for the next run!");
			return false;
		}
	}

	public abstract Set<String> getAllTopics();

	public abstract long getNumTopics();
	
	public abstract T getTopicInfoFromBroker(String topicName);

	public void stop() {
		if (executorService != null)
			executorService.shutdown();
	}

}
