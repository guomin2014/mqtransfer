package com.gm.mqtransfer.facade.filter;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import com.gm.mqtransfer.facade.model.TransferTaskFilter;

public class FilterFactory {

	public static Filter createFilter(TransferTaskFilter filterConfig) {
		if (filterConfig == null || !filterConfig.isEnable()) {
			return null;
		}
		FilterMode mode = FilterMode.getByValue(filterConfig.getMode());
		if (mode == null) {
			throw new IllegalArgumentException("Unknow mode[" + filterConfig.getMode() + "]");
		}
		switch (mode) {
		case HEADER:
			String rule = filterConfig.getRule();//按消息头过滤的表达式，如：${key1}=a&&${key2}=b
			return new MessageHeaderFilter(rule);
		case CUSTOM:
			String handler = filterConfig.getHandler();
			try {
				Class<?> clazz = Class.forName(handler);
				if (Filter.class.isAssignableFrom(clazz)) {
					Constructor<?> constructor = clazz.getConstructor(String.class);
					Filter filter = (Filter)constructor.newInstance(filterConfig.getRule());
					return filter;
				} else {
					throw new IllegalArgumentException("Parameter[" + handler + "] is not an instance of Filter");
				}
			} catch (Exception e) {
				throw new IllegalArgumentException("Class [" + handler + "] load error", e);
			}
		}
		return null;
	}
	
	public static FilterChain createFilters(List<TransferTaskFilter> filterConfigs) {
		if (filterConfigs == null || filterConfigs.isEmpty()) {
			return null;
		}
		List<Filter> filterList = new ArrayList<>();
		for (TransferTaskFilter config : filterConfigs) {
			Filter filter = createFilter(config);
			if (filter != null) {
				filterList.add(filter);
			}
		}
		FilterChain filterChain = new FilterChain(filterList);
		return filterChain;
	}
}
