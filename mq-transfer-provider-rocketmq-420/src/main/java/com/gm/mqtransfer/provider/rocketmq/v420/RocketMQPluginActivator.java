package com.gm.mqtransfer.provider.rocketmq.v420;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alipay.sofa.ark.spi.model.PluginContext;
import com.alipay.sofa.ark.spi.service.PluginActivator;
import com.gm.mqtransfer.provider.facade.service.PluginProviderService;
import com.gm.mqtransfer.provider.rocketmq.v420.service.RocketMQ420ProviderService;

public class RocketMQPluginActivator implements PluginActivator{

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	@Override
	public void start(PluginContext context) {
		logger.info("Starting the rocketmq v420 plugin ..." + context.getPlugin().getPluginName());
		context.publishService(PluginProviderService.class, new RocketMQ420ProviderService(), "rkt420");
	}

	@Override
	public void stop(PluginContext context) {
		logger.info("Stoping the rocketmq v420 plugin ...");
	}
}
