package com.gm.mqtransfer.provider.kafka.v230;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alipay.sofa.ark.spi.model.PluginContext;
import com.alipay.sofa.ark.spi.service.PluginActivator;
import com.gm.mqtransfer.provider.facade.service.PluginProviderService;
import com.gm.mqtransfer.provider.kafka.v230.service.Kafka230ProviderService;

public class KafkaPluginActivator implements PluginActivator{

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	@Override
	public void start(PluginContext context) {
		logger.info("Starting the kafka v230 plugin ..." + context.getPlugin().getPluginName());
		context.publishService(PluginProviderService.class, new Kafka230ProviderService(), "kfk230");
	}

	@Override
	public void stop(PluginContext context) {
		logger.info("Stoping the kafka v230 plugin ...");
	}

}
