package com.gm.mqtransfer.facade.filter;

import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import com.gm.mqtransfer.facade.model.CustomMessage;
import com.gm.mqtransfer.provider.facade.util.StringUtils;

public class MessageHeaderFilter implements Filter {
	
	private String filterRule;
	
	private ScriptEngine engine;

	public MessageHeaderFilter(String filterRule) {
		this.filterRule = filterRule;
		this.engine = new ScriptEngineManager().getEngineByName("javascript");
	}
	@Override
	public boolean doFilter(CustomMessage msg) {
		if (StringUtils.isBlank(filterRule)) {
			return true;
		}
		//替换变量
		Map<String, String> header = msg.getHeaders();
		if (header == null || header.isEmpty()) {
			return false;
		}
		String expression = this.filterRule;
		for (Map.Entry<String, String> entry : header.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			expression = expression.replace("${" + key + "}", value);
		}
		try {
			Object result = engine.eval(expression);
			if (result instanceof Boolean) {
				return ((Boolean)result).booleanValue();
			} else {
				return false;
			}
		} catch (Exception e) {
			return false;
		}
		
	}

}
