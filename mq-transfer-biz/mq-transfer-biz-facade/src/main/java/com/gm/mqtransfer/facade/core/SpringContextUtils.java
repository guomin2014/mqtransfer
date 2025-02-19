package com.gm.mqtransfer.facade.core;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.ApplicationContext;

public class SpringContextUtils {

	/** ark容器 */
	private static ApplicationContext context;
	/** 本地容器 */
	private static Map<String, ApplicationContext> innerContextMap = new HashMap<>();
	
	public static void updateContext(ApplicationContext context) {
		SpringContextUtils.context = context;
	}
	
	public static void updateInnerContext(String key, ApplicationContext context) {
		SpringContextUtils.innerContextMap.put(key, context);
	}
	
	public static <T> T getBean(Class<T> clazz) {
		if (context != null) {
			try {
				return context.getBean(clazz);
			} catch (Exception e) {}
		}
		return null;
	}
	/**
	 * 从多个容器中获取Bean，优先从本地容器获取
	 * @param key
	 * @param clazz
	 * @return
	 */
	public static <T> T getBean(String key, Class<T> clazz) {
		T t = null;
		ApplicationContext innerContext = innerContextMap.get(key);
		if (innerContext != null) {
			try {
				t = innerContext.getBean(clazz);
			} catch (Exception e) {}
		}
		if (t == null) {
			if (context != null) {
				try {
					t = context.getBean(clazz);
				} catch (Exception e) {}
			}
		}
		return t;
	}
}
