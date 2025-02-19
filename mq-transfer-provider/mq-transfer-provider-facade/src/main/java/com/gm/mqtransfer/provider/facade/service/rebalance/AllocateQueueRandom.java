package com.gm.mqtransfer.provider.facade.service.rebalance;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import com.gm.mqtransfer.provider.facade.model.TopicPartitionInfo;
import com.gm.mqtransfer.provider.facade.model.TopicPartitionMapping;

public class AllocateQueueRandom implements AllocateQueueStrategy {

	public static final String name = "RANDOM";
	
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
        List<TopicPartitionMapping> allocateList = new ArrayList<>();
		for (int i = 0; i < fromPartitionList.size(); i++) {
			TopicPartitionInfo fromPartition = fromPartitionList.get(i);
			int toPartitionIndex = ThreadLocalRandom.current().nextInt(toPartitionList.size());
			TopicPartitionInfo toPartition = toPartitionList.get(toPartitionIndex);
			allocateList.add(new TopicPartitionMapping(fromPartition, toPartition));
		}
		return allocateList;
	}

}
