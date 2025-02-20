package com.gm.mqtransfer.worker.filter;

import com.gm.mqtransfer.facade.filter.Convert;
import com.gm.mqtransfer.facade.model.CustomMessage;

public class DynamicMessageConvert implements Convert {

	private String convertRule;
	

	public DynamicMessageConvert(String convertRule) {
		this.convertRule = convertRule;
	}
	
	@Override
	public CustomMessage doConvert(CustomMessage message) {
		return message;
	}

}
