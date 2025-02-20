package com.gm.mqtransfer.provider.facade.service.rebalance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.gm.mqtransfer.provider.facade.model.TopicPartitionInfo;
import com.gm.mqtransfer.provider.facade.model.TopicPartitionMapping;
/**
 * match partition room, such as 1->1,2->2
 * @author GM
 * @date 2024-05-13
 */
public class AllocateQueueByPartitionRoom implements AllocateQueueStrategy {

	public static final String name = "PARTITION_ROOM";
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public List<TopicPartitionMapping> allocate(List<TopicPartitionInfo> fromPartitionList, List<TopicPartitionInfo> toPartitionList) {
		if (fromPartitionList == null || fromPartitionList.isEmpty()) {
			throw new IllegalArgumentException("Source partition not found");
		}
		if (toPartitionList == null || toPartitionList.isEmpty()) {
			throw new IllegalArgumentException("Target partition not found");
		}
		Map<String, TopicPartitionInfo> toPartitionMap = toPartitionList.stream()
				.collect(Collectors.toMap(e -> e.getPartitionKey(), e -> e, (k1, k2) -> k1));
		List<TopicPartitionMapping> allocateList = new ArrayList<>();
		for (int i = 0; i < fromPartitionList.size(); i++) {
			TopicPartitionInfo fromPartition = fromPartitionList.get(i);
			TopicPartitionInfo toPartition = toPartitionMap.get(fromPartition.getPartitionKey());
			allocateList.add(new TopicPartitionMapping(fromPartition, toPartition));
		}
		return allocateList;
	}

}
