package com.gm.mqtransfer.provider.facade.service.rebalance;

import java.util.Properties;

import com.gm.mqtransfer.provider.facade.util.StringUtils;

public class AllocateQueueStrategyFactory {

	/**
	 * 
	 * @param strategy
	 * @return
	 */
	public static AllocateQueueStrategy getAllocateQueueStrategy(String allocateStrategy, Properties allocateConfig) {
		AllocateQueueStrategy allocateQueueStrategy = null;
		if (StringUtils.isBlank(allocateStrategy)) {//默认策略：平均
			allocateQueueStrategy = new AllocateQueueAveragelyByCircle();
		} else {
			allocateStrategy = allocateStrategy.trim().toUpperCase();
			if (AllocateQueueAveragelyByCircle.name.equalsIgnoreCase(allocateStrategy)) {
				allocateQueueStrategy = new AllocateQueueAveragelyByCircle();
			} else if (AllocateQueueByConfig.name.equalsIgnoreCase(allocateStrategy)) {
				allocateQueueStrategy = new AllocateQueueByConfig(allocateConfig);
			} else if (AllocateQueueByPartitionRoom.name.equalsIgnoreCase(allocateStrategy)) {
				allocateQueueStrategy = new AllocateQueueByPartitionRoom();
			} else if (AllocateQueueHash.name.equalsIgnoreCase(allocateStrategy)) {
				allocateQueueStrategy = new AllocateQueueHash();
			} else if (AllocateQueueRandom.name.equalsIgnoreCase(allocateStrategy)) {
				allocateQueueStrategy = new AllocateQueueRandom();
			} else {
				throw new IllegalArgumentException("Unknow allocate strategy[" + allocateStrategy + "]");
			}
		}
		return allocateQueueStrategy;
	}
}
