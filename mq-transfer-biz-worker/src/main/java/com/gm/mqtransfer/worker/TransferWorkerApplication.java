package com.gm.mqtransfer.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import com.gm.mqtransfer.worker.core.SpringContextWorkerUtils;

@SpringBootApplication(scanBasePackages= {"com.gm.mqtransfer.facade", "com.gm.mqtransfer.module", "com.gm.mqtransfer.worker"})
public class TransferWorkerApplication {

	private static final Logger logger = LoggerFactory.getLogger(TransferWorkerApplication.class);
	
	public static void main(String[] args) {
		logger.info("starting worker ... ...");
//		ConfigurableApplicationContext context = SpringApplication.run(TransferWorkerApplication.class, args);
//		SpringContextWorkerUtils.updateContext(context);
		SpringApplicationBuilder builder = new SpringApplicationBuilder(TransferWorkerApplication.class);
		ResourceLoader resourceLoader = new DefaultResourceLoader(TransferWorkerApplication.class.getClassLoader());
		builder.resourceLoader(resourceLoader);
		ConfigurableApplicationContext context = builder.build().run(args);
		SpringContextWorkerUtils.updateContext(context);
	}

}
