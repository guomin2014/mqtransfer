package com.gm.mqtransfer.provider.kafka.v082;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alipay.sofa.ark.spi.model.PluginContext;
import com.alipay.sofa.ark.spi.service.PluginActivator;
import com.gm.mqtransfer.provider.facade.service.PluginProviderService;
import com.gm.mqtransfer.provider.kafka.v082.service.Kafka082ProviderService;

public class KafkaPluginActivator implements PluginActivator{

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	@Override
	public void start(PluginContext context) {
		logger.info("Starting the kafka v082 plugin ..." + context.getPlugin().getPluginName());
		context.publishService(PluginProviderService.class, new Kafka082ProviderService(), "kfk082");
	}

	@Override
	public void stop(PluginContext context) {
		logger.info("Stoping the kafka v082 plugin ...");
	}

}
