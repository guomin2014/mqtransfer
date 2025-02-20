package com.gm.mqtransfer.worker.core;

import org.springframework.context.ApplicationContext;

import com.gm.mqtransfer.facade.core.SpringContextUtils;

public class SpringContextWorkerUtils {

	/** 容器 */
	private static ApplicationContext context;
	
	public static void updateContext(ApplicationContext context) {
		SpringContextWorkerUtils.context = context;
	}
	/**
	 * 从多个容器中获取Bean，优先从本地容器获取
	 * @param clazz
	 * @return
	 */
	public static <T> T getBean(Class<T> clazz) {
		T t = null;
		if (context != null) {
			try {
				t = context.getBean(clazz);
			} catch (Exception e) {}
		}
		if (t == null) {
			t = SpringContextUtils.getBean(clazz);
		}
		return t;
	}
}
