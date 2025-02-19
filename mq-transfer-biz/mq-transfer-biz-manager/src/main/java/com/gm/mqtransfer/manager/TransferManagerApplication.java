package com.gm.mqtransfer.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages= {"com.gm.mqtransfer.facade", "com.gm.mqtransfer.module", "com.gm.mqtransfer.manager"})
public class TransferManagerApplication {

	private static final Logger logger = LoggerFactory.getLogger(TransferManagerApplication.class);
	
	public static void main(String[] args) {
//		ConfigurableApplicationContext context = SpringApplication.run(MqTransferManagerApplication.class, args);
//		System.out.println("MqTransferManagerApplication start!");
//		System.out.println("Spring Boot Version: " + SpringApplication.class.getPackage().getImplementationVersion());
//		System.out.println("MqTransferManagerApplication classLoader: " + MqTransferManagerApplication.class.getClassLoader());
//		TestService service = context.getBean(TestService.class);
//		System.out.println("bean:" + service);
//		if (service != null) {
//			service.service();
//		}
		logger.info("starting manager ... ...");
		SpringApplication.run(TransferManagerApplication.class, args);
	}

}
