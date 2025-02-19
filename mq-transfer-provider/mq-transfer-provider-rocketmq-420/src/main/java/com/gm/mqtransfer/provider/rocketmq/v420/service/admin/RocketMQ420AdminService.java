package com.gm.mqtransfer.provider.rocketmq.v420.service.admin;

import java.util.ArrayList;
import java.util.List;

import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.protocol.route.QueueData;
import org.apache.rocketmq.common.protocol.route.TopicRouteData;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gm.mqtransfer.provider.facade.model.RocketMQClusterInfo;
import com.gm.mqtransfer.provider.facade.model.TopicInfo;
import com.gm.mqtransfer.provider.facade.model.TopicPartitionInfo;
import com.gm.mqtransfer.provider.facade.service.admin.AdminService;

public class RocketMQ420AdminService implements AdminService {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(RocketMQ420AdminService.class);
	
	private DefaultMQAdminExt adminExt;

	public RocketMQ420AdminService(RocketMQClusterInfo clusterInfo) {
		LOGGER.info("Trying to init RocketMQ420AdminService {}({}) with nameSvrAddr: {}", clusterInfo.getName(), clusterInfo.getCode(), clusterInfo.getNameSvrAddr());
		String nameSvrAddr = clusterInfo.getNameSvrAddr();
		DefaultMQAdminExt adminExt = new DefaultMQAdminExt();
	    adminExt.setNamesrvAddr(nameSvrAddr);
	    adminExt.setInstanceName(System.currentTimeMillis() + "");
	    try {
	    	adminExt.start();
	    } catch (MQClientException e) {
	    	LOGGER.error("create rocketmq admin error:", e);
	    	throw new RuntimeException(e);
	    	
	    }
	}
	@Override
	public TopicInfo getTopicInfo(String topic) {
		try {
			TopicRouteData data = adminExt.examineTopicRouteInfo(topic);
			List<QueueData> queueList = data.getQueueDatas();
			List<TopicPartitionInfo> partitionList = new ArrayList<>();
			for (QueueData queue : queueList) {
				for (int i = 0; i < queue.getReadQueueNums(); i++) {
					partitionList.add(new TopicPartitionInfo(i, queue.getBrokerName()));
				}
			}
			return new TopicInfo(topic, partitionList);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public void stop() {
		if (adminExt != null) {
			adminExt.shutdown();
		}
	}

}
