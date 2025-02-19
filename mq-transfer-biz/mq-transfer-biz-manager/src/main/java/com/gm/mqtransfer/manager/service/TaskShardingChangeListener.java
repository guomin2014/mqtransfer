package com.gm.mqtransfer.manager.service;

import java.util.List;

import com.gm.mqtransfer.facade.model.TransferTask;
import com.gm.mqtransfer.provider.facade.model.TopicPartitionInfo;

/**
 * Interface to implement to listen for topic`s partition changes.
 * @author GM
 * @date 2024-08-26
 *
 */
public interface TaskShardingChangeListener {

	/**
	 * Invoked when partiton changes
	 * @param task
	 * @param oldPartitionList
	 * @param newPartitionList
	 */
	void onPartitionChange(TransferTask task, List<TopicPartitionInfo> oldPartitionList, List<TopicPartitionInfo> newPartitionList);

}
