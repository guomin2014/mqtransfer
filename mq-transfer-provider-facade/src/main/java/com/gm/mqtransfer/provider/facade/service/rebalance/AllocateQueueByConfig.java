package com.gm.mqtransfer.provider.facade.service.rebalance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import com.gm.mqtransfer.provider.facade.model.TopicPartitionInfo;
import com.gm.mqtransfer.provider.facade.model.TopicPartitionMapping;

public class AllocateQueueByConfig implements AllocateQueueStrategy {

	public static final String name = "CONFIG";
	
	Properties matchPartitionMap;
	
	public AllocateQueueByConfig(Properties matchPartitionMap) {
		this.matchPartitionMap = matchPartitionMap;
	}
	
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
		if (matchPartitionMap == null || matchPartitionMap.isEmpty()) {
			throw new IllegalArgumentException("Match partition not found");
		}
		Map<String, TopicPartitionInfo> toPartitionMap = toPartitionList.stream()
				.collect(Collectors.toMap(e -> e.getPartitionKey(), e -> e, (k1, k2) -> k1));
		List<TopicPartitionMapping> allocateList = new ArrayList<>();
		for (int i = 0; i < fromPartitionList.size(); i++) {
			TopicPartitionInfo fromPartition = fromPartitionList.get(i);
			Object toPartitionKey = matchPartitionMap.get(fromPartition.getPartitionKey());
			TopicPartitionInfo toPartition = toPartitionKey != null ? toPartitionMap.get(toPartitionKey.toString()) : null;
			allocateList.add(new TopicPartitionMapping(fromPartition, toPartition));
		}
		return allocateList;
	}

}
