package com.gm.mqtransfer.provider.rocketmq.v420.service;

import com.gm.mqtransfer.provider.facade.model.ClusterInfo;
import com.gm.mqtransfer.provider.facade.model.ConsumerClientConfig;
import com.gm.mqtransfer.provider.facade.model.MQMessage;
import com.gm.mqtransfer.provider.facade.model.ProducerClientConfig;
import com.gm.mqtransfer.provider.facade.model.RocketMQClusterInfo;
import com.gm.mqtransfer.provider.facade.model.ServiceDesc;
import com.gm.mqtransfer.provider.facade.service.PluginProviderService;
import com.gm.mqtransfer.provider.facade.service.ProviderServiceFactory;
import com.gm.mqtransfer.provider.facade.service.admin.AdminService;
import com.gm.mqtransfer.provider.facade.service.consumer.ConsumerService;
import com.gm.mqtransfer.provider.facade.service.producer.ProducerService;
import com.gm.mqtransfer.provider.rocketmq.v420.service.admin.RocketMQ420AdminService;
import com.gm.mqtransfer.provider.rocketmq.v420.service.consumer.RocketMQ420ConsumerService;
import com.gm.mqtransfer.provider.rocketmq.v420.service.producer.RocketMQ420ProducerService;

public class RocketMQ420ProviderService implements PluginProviderService {

	@Override
	public ServiceDesc serviceDesc() {
		ServiceDesc desc = new ServiceDesc();
		desc.setMqName("rocketmq#4.2.0");
		desc.setMqType("rocketmq");
		desc.setMqVersion("4.2.0");
		return desc;
	}
	
	private RocketMQClusterInfo convertClusterInfo(ClusterInfo clusterInfo) {
		RocketMQClusterInfo rocketMQClusterInfo = null;
		if (clusterInfo instanceof RocketMQClusterInfo) {
			rocketMQClusterInfo = (RocketMQClusterInfo)clusterInfo;
		} else {
			rocketMQClusterInfo = (RocketMQClusterInfo)clusterInfo.toPluginClusterInfo();
		}
		return rocketMQClusterInfo;
	}

	@Override
	public ProviderServiceFactory getServiceFactory() {
		return new ProviderServiceFactory() {

			@Override
			public ConsumerService createConsumerService(ConsumerClientConfig config) {
				return new RocketMQ420ConsumerService(config);
			}

			@Override
			public <T extends MQMessage> ProducerService<T> createProducerService(ProducerClientConfig config) {
				return new RocketMQ420ProducerService<>(config);
			}

			@Override
			public AdminService createAdminService(ClusterInfo clusterInfo) {
				RocketMQClusterInfo rocketMQClusterInfo = convertClusterInfo(clusterInfo);
				return new RocketMQ420AdminService(rocketMQClusterInfo);
			}
		};

	}

}
