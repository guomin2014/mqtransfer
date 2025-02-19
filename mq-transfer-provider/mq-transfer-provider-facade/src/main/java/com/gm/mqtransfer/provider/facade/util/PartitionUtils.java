package com.gm.mqtransfer.provider.facade.util;

import com.gm.mqtransfer.provider.facade.model.TopicPartitionInfo;

public class PartitionUtils {

	/** key内部分隔符 */
	public final static String KEY_INNER_SPLIT = "#";
	/** key之间的分隔符 */
	public final static String KEY_OUTER_SPLIT = "-";
	/**
	 * 创建分区KEY
	 * @param brokerName
	 * @param partition
	 * @return
	 */
	public static String generatorPartitionKey(String brokerName, Integer partition) {
		return (StringUtils.isBlank(brokerName) ? "" : brokerName + KEY_INNER_SPLIT) + (partition == null ? "" : partition.toString());
	}
	public static String generatorTopicPartitionKey(String topic, String brokerName, Integer partition) {
		return topic + KEY_INNER_SPLIT + (StringUtils.isBlank(brokerName) ? "" : brokerName + KEY_INNER_SPLIT) + (partition == null ? "" : partition.toString());
	}
	public static String generatorResourcePartitionKey(String resourceName, String partitionKey) {
		return resourceName + KEY_INNER_SPLIT + (StringUtils.isBlank(partitionKey) ? "" : partitionKey);
	}
	public static String generatorResourcePartitionKey(String resourceName, String brokerName, Integer partition) {
		return resourceName + KEY_INNER_SPLIT + (StringUtils.isBlank(brokerName) ? "" : brokerName + KEY_INNER_SPLIT) + (partition == null ? "" : partition.toString());
	}
	/**
	 * 
	 * @param partitionKey
	 * @return	[0]:brokerName,[1]:partition
	 */
	public static TopicPartitionInfo parsePartitionKey(String partitionKey) {
		if (StringUtils.isBlank(partitionKey)) {
			return null;
		}
		String[] keys = partitionKey.split(KEY_INNER_SPLIT);
		if (keys.length == 1) {
			return new TopicPartitionInfo(Integer.parseInt(keys[0]));
		} else if (keys.length == 2) {
			return new TopicPartitionInfo(Integer.parseInt(keys[1]), keys[0]);
		} else {
			return null;
		}
	}
}
