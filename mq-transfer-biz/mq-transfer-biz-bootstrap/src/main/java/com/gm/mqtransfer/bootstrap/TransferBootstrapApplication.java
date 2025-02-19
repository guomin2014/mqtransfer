package com.gm.mqtransfer.bootstrap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

import com.gm.mqtransfer.facade.service.IApplicationService;

@SpringBootApplication(scanBasePackages= {"com.gm.mqtransfer.bootstrap","com.gm.mqtransfer.facade"})
@ComponentScan(excludeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.gm.mqtransfer.manager.*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.gm.mqtransfer.worker.*")
})
public class TransferBootstrapApplication {

	private static final Logger logger = LoggerFactory.getLogger(TransferBootstrapApplication.class);
	
	public static void main(String[] args) {
//		logger.info("starting core ... ...");
//        SpringApplication.run(TransferBootstrapApplication.class, args);
        ConfigurableApplicationContext context = SpringApplication.run(TransferBootstrapApplication.class, args);
        logger.info("TransferBootstrapApplication start!");
        logger.info("Spring Boot Version: " + SpringApplication.class.getPackage().getImplementationVersion());
        logger.info("TransferBootstrapApplication classLoader: " + TransferBootstrapApplication.class.getClassLoader());
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            private volatile boolean hasShutdown = false;
            private AtomicInteger shutdownTimes = new AtomicInteger(0);
            @Override
            public void run() {
                synchronized (this) {
                	logger.info("Shutdown hook was invoked, {}", this.shutdownTimes.incrementAndGet());
                    if (!this.hasShutdown) {
                        this.hasShutdown = true;
                        long beginTime = System.currentTimeMillis();
                        Map<String, IApplicationService> map = context.getBeansOfType(IApplicationService.class);
                        if (map != null) {
                        	List<IApplicationService> services = new ArrayList<>(map.values());
                        	Collections.sort(services, new Comparator<IApplicationService>() {
                				@Override
                				public int compare(IApplicationService o1, IApplicationService o2) {
                					return o1.order() > o2.order() ? -1 : 1;
                				}
                			});
                        	for (IApplicationService service : services) {
                        		service.start();
                        	}
                        }
                        long consumingTimeTotal = System.currentTimeMillis() - beginTime;
                        logger.info("Shutdown hook over, consuming total time(ms): {}", consumingTimeTotal);
                    }
                }
            }
        }, "ShutdownHook"));
	}
	
}
