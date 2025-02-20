package com.gm.mqtransfer.provider.facade.service.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gm.mqtransfer.provider.facade.model.ClusterInfo;
import com.gm.mqtransfer.provider.facade.model.ConsumerClientConfig;
import com.gm.mqtransfer.provider.facade.model.ConsumerPartition;
import com.gm.mqtransfer.provider.facade.service.AbstractService;
import com.gm.mqtransfer.provider.facade.util.StringUtils;

public abstract class AbstractConsumerService extends AbstractService implements ConsumerService {
	
	protected ConsumerClientConfig consumerConfig;
	
	protected final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	public AbstractConsumerService(ConsumerClientConfig consumerConfig) {
		this.consumerConfig = consumerConfig;
	}

	public String generateConsumerKey(ConsumerPartition task) {
		return this.generateConsumerKey(task.getFromTopic(), task.getFromPartition(), task.getFromBrokerName());
	}
	public String generateConsumerKey(String topic, Integer partition) {
		return this.generateConsumerKey(topic, partition, null);
	}
	public String generateConsumerKey(String topic, Integer partition, String brokerName) {
		return topic + "#" + partition + "#" + (StringUtils.isBlank(brokerName) ? "" : brokerName);
	}
	public String generateConsumerGroup() {
		//自定义消费者组格式：consumer_group_任务ID
		return "group_mqtransfer_" + this.consumerConfig.getInstanceCode();
	}
	public String generateInstanceName() {
		return "c_" + com.gm.mqtransfer.provider.facade.common.Constants.DEF_INSTANCE_ID_VAL + "_" + this.consumerConfig.getInstanceIndex();
	}
	
	@Override
	public boolean isShare() {
		return this.consumerConfig.isClientShare();
	}
	
	@Override
	public ClusterInfo getClusterInfo() {
		return this.consumerConfig.getCluster();
	}
	
	public String getConsumerGroup() {
		String consumerGroup = consumerConfig.getConsumerGroup();
		if (StringUtils.isBlank(consumerGroup)) {
			consumerGroup = generateConsumerGroup();
		}
		return consumerGroup;
	}
	
}
