package com.gm.mqtransfer.bootstrap.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

/**
 * 事件监听，在spring.factories中配置
 * 使用@Component将会丢失部份事件
 * @author GM
 * @date 2024-05-13
 */
public class TransferEventListener implements ApplicationListener<ApplicationEvent> {

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	private static final String APPLICATION_ENVIRONMENT_PREPARED_EVENT  = "org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent";
	
	/** 配置的属性名 */
    public static String CLUSTER_MODE_KEY = "cluster.mode";
	
	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (APPLICATION_ENVIRONMENT_PREPARED_EVENT.equals(event.getClass().getCanonicalName())) {
			ApplicationEnvironmentPreparedEvent e = (ApplicationEnvironmentPreparedEvent)event;
			logger.info("************cluster.mode: " + e.getEnvironment().getProperty(CLUSTER_MODE_KEY) + "**********");
			System.setProperty(CLUSTER_MODE_KEY, e.getEnvironment().getProperty(CLUSTER_MODE_KEY));//更新到环境变量中
		}
	}

}
