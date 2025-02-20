package com.gm.mqtransfer.provider.facade.service;

import com.gm.mqtransfer.provider.facade.model.ClusterInfo;
import com.gm.mqtransfer.provider.facade.model.ConsumerClientConfig;
import com.gm.mqtransfer.provider.facade.model.MQMessage;
import com.gm.mqtransfer.provider.facade.model.ProducerClientConfig;
import com.gm.mqtransfer.provider.facade.service.admin.AdminService;
import com.gm.mqtransfer.provider.facade.service.consumer.ConsumerService;
import com.gm.mqtransfer.provider.facade.service.producer.ProducerService;

public interface ProviderServiceFactory {
	/**
	 * Create a cluster management service
	 * @param clusterInfo
	 * @return
	 */
	public AdminService createAdminService(ClusterInfo clusterInfo);
	/**
	 * Create a consumer service
	 * @param clusterInfo
	 * @param consumerProps
	 * @param instanceIndex
	 * @param instanceCode
	 * @param clientShare
	 * @return
	 */
	public ConsumerService createConsumerService(ConsumerClientConfig consumerConfig);
	/**
	 * Create a producer service
	 * @param clusterInfo
	 * @param producerProps
	 * @param instanceIndex
	 * @param instanceCode
	 * @param clientShare
	 * @return
	 */
	public <T extends MQMessage> ProducerService<T> createProducerService(ProducerClientConfig producerConfig);
}
