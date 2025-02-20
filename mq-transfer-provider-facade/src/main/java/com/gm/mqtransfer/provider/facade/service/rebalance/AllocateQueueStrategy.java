package com.gm.mqtransfer.provider.facade.service.rebalance;

import java.util.List;
import java.util.Properties;

import com.gm.mqtransfer.provider.facade.model.TopicPartitionInfo;
import com.gm.mqtransfer.provider.facade.model.TopicPartitionMapping;

/**
 * Strategy Algorithm for message allocating between consumers
 * @author GM
 * @date Feb 23, 2023
 */
public interface AllocateQueueStrategy {

	/**
	 * Allocating by task
	 * @param fromPartitionList
	 * @param toPartitionList
	 * @return	partition mapping
	 */
	default List<TopicPartitionMapping> allocate(List<TopicPartitionInfo> fromPartitionList, List<TopicPartitionInfo> toPartitionList) {
		return null;
	}
	/**
	 * Allocating by task
	 * @param fromPartitionList
	 * @param toPartitionList
	 * @param matchPartitionMap		Partition matching relationship，key:fromPartitionKey，value:toPartitionKey
	 * @return
	 */
	default List<TopicPartitionMapping> allocate(List<TopicPartitionInfo> fromPartitionList, List<TopicPartitionInfo> toPartitionList, Properties matchPartitionMap) {
		return new AllocateQueueByConfig(matchPartitionMap).allocate(fromPartitionList, toPartitionList);
	}
	/**
     * Algorithm name
     *
     * @return The strategy name
     */
    String getName();
}