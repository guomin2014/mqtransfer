package com.gm.mqtransfer.provider.facade.service.rebalance;

import java.util.List;
import java.util.Properties;

import com.gm.mqtransfer.provider.facade.model.TopicPartitionInfo;
import com.gm.mqtransfer.provider.facade.model.TopicPartitionMapping;

public class AllocateQueueUtil {

	public static List<TopicPartitionMapping> allocatePartition(String allocateStrategy, Properties allocateConfig, List<TopicPartitionInfo> fromPartitionList, List<TopicPartitionInfo> toPartitionList) {
		AllocateQueueStrategy allocateQueueStrategy = AllocateQueueStrategyFactory.getAllocateQueueStrategy(allocateStrategy, allocateConfig);
		if (allocateQueueStrategy == null) {
			throw new IllegalArgumentException("Allocate strategy not found");
		}
		//根据读取消息的分区，查找发送消息的分区
		List<TopicPartitionMapping> allocateList = allocateQueueStrategy.allocate(fromPartitionList, toPartitionList);
        if (allocateList == null) {
        	throw new IllegalArgumentException("No matching partition found");
        }
        return allocateList;
	}
}
