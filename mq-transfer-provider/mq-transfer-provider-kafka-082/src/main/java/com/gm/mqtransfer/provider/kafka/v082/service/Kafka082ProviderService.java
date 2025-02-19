package com.gm.mqtransfer.provider.kafka.v082.service;

import java.util.Properties;

import com.gm.mqtransfer.provider.facade.model.ClusterInfo;
import com.gm.mqtransfer.provider.facade.model.ConsumerClientConfig;
import com.gm.mqtransfer.provider.facade.model.KafkaClusterInfo;
import com.gm.mqtransfer.provider.facade.model.MQMessage;
import com.gm.mqtransfer.provider.facade.model.ProducerClientConfig;
import com.gm.mqtransfer.provider.facade.model.ServiceDesc;
import com.gm.mqtransfer.provider.facade.service.PluginProviderService;
import com.gm.mqtransfer.provider.facade.service.ProviderServiceFactory;
import com.gm.mqtransfer.provider.facade.service.admin.AdminService;
import com.gm.mqtransfer.provider.facade.service.consumer.ConsumerService;
import com.gm.mqtransfer.provider.facade.service.producer.ProducerService;
import com.gm.mqtransfer.provider.kafka.v082.service.admin.Kafka082AdminService;
import com.gm.mqtransfer.provider.kafka.v082.service.consumer.Kafka082ConsumerService;
import com.gm.mqtransfer.provider.kafka.v082.service.producer.Kafka082ProducerService;

public class Kafka082ProviderService implements PluginProviderService {

//	@ArkInject
//    private EventAdminService eventAdminService;
//	
//	public void sendEvent(ArkEvent arkEvent) {
//        eventAdminService.sendEvent(arkEvent);
//    }

//    public String service() {
//    	if (eventAdminService != null) {
//    		System.out.println(eventAdminService);
//    	}
//        return "I'm a sample plugin service published by kafka082";
//    }

	@Override
	public ServiceDesc serviceDesc() {
		ServiceDesc desc = new ServiceDesc();
		desc.setMqName("kafka#0.8.2");
		desc.setMqType("kafka");
		desc.setMqVersion("0.8.2");
		return desc;
	}
	
	private KafkaClusterInfo convertClusterInfo(ClusterInfo clusterInfo) {
		KafkaClusterInfo kafkaClusterInfo = null;
		if (clusterInfo instanceof KafkaClusterInfo) {
			kafkaClusterInfo = (KafkaClusterInfo)clusterInfo;
		} else {
			kafkaClusterInfo = (KafkaClusterInfo)clusterInfo.toPluginClusterInfo();
		}
		return kafkaClusterInfo;
	}

	@Override
	public ProviderServiceFactory getServiceFactory() {
		return new ProviderServiceFactory() {

			@Override
			public ConsumerService createConsumerService(ConsumerClientConfig config) {
				return new Kafka082ConsumerService(config);
			}

			@Override
			public <T extends MQMessage> ProducerService<T> createProducerService(ProducerClientConfig config) {
				return new Kafka082ProducerService<>(config);
			}

			@Override
			public AdminService createAdminService(ClusterInfo clusterInfo) {
				KafkaClusterInfo kafkaClusterInfo = convertClusterInfo(clusterInfo);
				return new Kafka082AdminService(kafkaClusterInfo);
			}};
	}

}
