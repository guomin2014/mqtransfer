package com.gm.mqtransfer.manager.support.helix;

import java.util.List;

import org.apache.helix.NotificationContext;
import org.apache.helix.api.listeners.LiveInstanceChangeListener;
import org.apache.helix.model.LiveInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * We only considering add or remove box(es), not considering the replacing. For
 * replacing, we just need to bring up a new box and give the old instanceId no
 * auto-balancing needed.
 */
public class CustomLiveInstanceChangeListener implements LiveInstanceChangeListener {
	private final Logger logger = LoggerFactory.getLogger(CustomLiveInstanceChangeListener.class);
	private final HelixBalanceManager balanceManager;

	public CustomLiveInstanceChangeListener(HelixBalanceManager balanceManager) {
		this.balanceManager = balanceManager;
	}

	@Override
	public void onLiveInstanceChange(List<LiveInstance> liveInstances, NotificationContext changeContext) {
		logger.info("Live instance change({}), automatic cluster balancing will be triggered!", liveInstances != null ? liveInstances.size() : 0);
		this.balanceManager.rebalance(liveInstances);
	}
}
