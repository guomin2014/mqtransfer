package com.gm.mqtransfer.provider.kafka.v230.service;

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
import com.gm.mqtransfer.provider.kafka.v230.service.admin.Kafka230AdminService;
import com.gm.mqtransfer.provider.kafka.v230.service.consumer.Kafka230ConsumerService;
import com.gm.mqtransfer.provider.kafka.v230.service.producer.Kafka230ProducerService;

public class Kafka230ProviderService implements PluginProviderService {

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
		desc.setMqName("kafka#2.3.0");
		desc.setMqType("kafka");
		desc.setMqVersion("2.3.0");
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
				return new Kafka230ConsumerService(config);
			}

			@Override
			public <T extends MQMessage> ProducerService<T> createProducerService(ProducerClientConfig config) {
				return new Kafka230ProducerService<>(config);
			}

			@Override
			public AdminService createAdminService(ClusterInfo clusterInfo) {
				KafkaClusterInfo kafkaClusterInfo = convertClusterInfo(clusterInfo);
				return new Kafka230AdminService(kafkaClusterInfo);
			}
		};

	}

}
