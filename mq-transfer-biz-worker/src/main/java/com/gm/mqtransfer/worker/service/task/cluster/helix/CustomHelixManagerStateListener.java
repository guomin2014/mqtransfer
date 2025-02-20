package com.gm.mqtransfer.worker.service.task.cluster.helix;

import org.apache.helix.HelixManager;
import org.apache.helix.manager.zk.HelixManagerStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CustomHelixManagerStateListener implements HelixManagerStateListener {

	private final Logger logger = LoggerFactory.getLogger(CustomHelixManagerStateListener.class);
	
	
	public CustomHelixManagerStateListener() {
	}

	@Override
	public void onConnected(HelixManager helixManager) throws Exception {
		logger.info("[{}] HelixManager successfully connected", helixManager.getClusterName());
	}

	@Override
	public void onDisconnected(HelixManager helixManager, Throwable error) throws Exception {
		logger.info("[{}] HelixManager disconnected-->{}", helixManager.getClusterName(), error.getMessage());
	}
	

}
