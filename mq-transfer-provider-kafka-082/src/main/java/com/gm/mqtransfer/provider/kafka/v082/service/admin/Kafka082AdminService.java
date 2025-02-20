package com.gm.mqtransfer.provider.kafka.v082.service.admin;

import java.util.ArrayList;
import java.util.List;

import org.I0Itec.zkclient.ZkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gm.mqtransfer.provider.facade.model.KafkaClusterInfo;
import com.gm.mqtransfer.provider.facade.model.TopicInfo;
import com.gm.mqtransfer.provider.facade.model.TopicPartitionInfo;
import com.gm.mqtransfer.provider.facade.service.admin.AdminService;

import kafka.utils.ZKStringSerializer$;

public class Kafka082AdminService implements AdminService {

	private static final Logger LOGGER = LoggerFactory.getLogger(Kafka082AdminService.class);
	private static String KAFKA_TOPICS_PATH = "/brokers/topics";
	private final ZkClient _kfkZkClient;
	
	public Kafka082AdminService(KafkaClusterInfo clusterInfo) {
		LOGGER.info("Trying to init Kafka082AdminService {}({}) with ZK: {}", clusterInfo.getName(), clusterInfo.getCode(), clusterInfo.getZkUrl());
		_kfkZkClient = new ZkClient(clusterInfo.getZkUrl(), 30000, 30000, ZKStringSerializer$.MODULE$);
	}

	@Override
	public TopicInfo getTopicInfo(String topic) {
		TopicInfo ti = null;
		try {
			String zkPath = String.format("%s/%s/partitions", KAFKA_TOPICS_PATH, topic);
			List<String> partitions = _kfkZkClient.getChildren(zkPath);
			List<TopicPartitionInfo> partitionList = new ArrayList<>();
			int n = 0;
			while(n < partitions.size()) {
				partitionList.add(new TopicPartitionInfo(n));
				n++;
			}
			ti = new TopicInfo(topic, partitionList);
			return ti;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void stop() {
		_kfkZkClient.close();
	}
}
