package com.gm.mqtransfer.provider.facade.service.rebalance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.gm.mqtransfer.provider.facade.model.TopicPartitionInfo;
import com.gm.mqtransfer.provider.facade.model.TopicPartitionMapping;
/**
 * Average Hashing queue algorithm
 * such as:
 * from partition: 1,2,3,4
 * to   partition: 1,2
 * allocate:  1,2 -> 1, 3,4 -> 2
 * from		to
 * 1		1
 * 2		1
 * 3		2
 * 4		2
 * @author GM
 * @date 2024-05-13
 */
public class AllocateQueueAveragely implements AllocateQueueStrategy {

	public static final String name = "AVG";
	
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
		//将目标分区按自然序列排序
		Collections.sort(fromPartitionList, new Comparator<TopicPartitionInfo>() {
			@Override
			public int compare(TopicPartitionInfo o1, TopicPartitionInfo o2) {
				return o1.getPartitionKey().compareTo(o2.getPartitionKey());
			}
		});
		Collections.sort(toPartitionList, new Comparator<TopicPartitionInfo>() {
			@Override
			public int compare(TopicPartitionInfo o1, TopicPartitionInfo o2) {
				return o1.getPartitionKey().compareTo(o2.getPartitionKey());
			}
		});
		List<TopicPartitionMapping> allocateList = new ArrayList<>();
		int mod = fromPartitionList.size() % toPartitionList.size();
		int averageSize =
				fromPartitionList.size() <= toPartitionList.size() ? 1 : (mod > 0 ? fromPartitionList.size() / toPartitionList.size()
	                + 1 : fromPartitionList.size() / toPartitionList.size());
		for (int i = 0; i < fromPartitionList.size(); i++) {
			TopicPartitionInfo fromPartition = fromPartitionList.get(i);
			TopicPartitionInfo toPartition = toPartitionList.get(i / averageSize);
			allocateList.add(new TopicPartitionMapping(fromPartition, toPartition));
		}
		return allocateList;
	}

}
