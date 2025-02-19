package com.gm.mqtransfer.facade.filter;

import java.util.List;

import com.gm.mqtransfer.facade.model.CustomMessage;

/**
 * 过滤链
 * @author GM
 *
 */
public class FilterChain {

	private List<Filter> filters;
	
	public FilterChain(List<Filter> filters) {
		this.filters = filters;
	}
	
	public boolean doFilter(CustomMessage msg) {
		if (filters == null || filters.isEmpty()) {
			return false;
		}
		for (Filter filter : filters) {
			if (filter.doFilter(msg)) {
				return true;
			}
		}
		return false;
	}
}
