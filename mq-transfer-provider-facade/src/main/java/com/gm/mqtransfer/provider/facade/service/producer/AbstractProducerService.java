package com.gm.mqtransfer.provider.facade.service.producer;

import com.gm.mqtransfer.provider.facade.api.ResponseBuilder;
import com.gm.mqtransfer.provider.facade.api.SubscribeData;
import com.gm.mqtransfer.provider.facade.api.producer.SubscribeRequest;
import com.gm.mqtransfer.provider.facade.api.producer.SubscribeResponse;
import com.gm.mqtransfer.provider.facade.api.producer.UnsubscribeRequest;
import com.gm.mqtransfer.provider.facade.api.producer.UnsubscribeResponse;
import com.gm.mqtransfer.provider.facade.model.ClusterInfo;
import com.gm.mqtransfer.provider.facade.model.MQMessage;
import com.gm.mqtransfer.provider.facade.model.ProducerClientConfig;
import com.gm.mqtransfer.provider.facade.model.ProducerPartition;
import com.gm.mqtransfer.provider.facade.service.AbstractService;

public abstract class AbstractProducerService<T extends MQMessage> extends AbstractService implements ProducerService<T> {

	private ProducerClientConfig producerConfig;
	
	public AbstractProducerService(ProducerClientConfig producerConfig) {
		this.producerConfig = producerConfig;
	}
	
	@Override
	public ClusterInfo getClusterInfo() {
		return this.producerConfig.getCluster();
	}
	
	@Override
	public boolean isShare() {
		return this.producerConfig.isClientShare();
	}
	
	public String generateInstanceName() {
		return "p_" + com.gm.mqtransfer.provider.facade.common.Constants.DEF_INSTANCE_ID_VAL + "_" + producerConfig.getInstanceIndex();
	}
	
	public String generateProducerGroup() {
		//自定义消费者组格式：consumer_group_任务ID
		return "group_mqtransfer_" + producerConfig.getInstanceCode();
	}
	
	@Override
	public SubscribeResponse subscribe(SubscribeRequest request) {
		ProducerPartition task = request.getPartition();
		logger.info("[{}] starting producer for task", task.getToDesc());
		return ResponseBuilder.toBuild(SubscribeResponse.class, new SubscribeData());
	}

	@Override
	public SubscribeResponse resubscribe(SubscribeRequest request) {
		ProducerPartition task = request.getPartition();
		logger.info("restarting producer for task-->" + task.getToDesc());
		return ResponseBuilder.toBuild(SubscribeResponse.class, new SubscribeData());
	}

	@Override
	public UnsubscribeResponse unsubscribe(UnsubscribeRequest request) {
		ProducerPartition task = request.getPartition();
		logger.info("[{}] stopping producer for task-->", task.getToDesc());
		return ResponseBuilder.toBuild(UnsubscribeResponse.class, new SubscribeData());
	}
}
