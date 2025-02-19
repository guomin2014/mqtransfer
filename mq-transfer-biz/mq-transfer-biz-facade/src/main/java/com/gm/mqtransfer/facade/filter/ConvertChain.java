package com.gm.mqtransfer.facade.filter;

import java.util.List;

import com.gm.mqtransfer.facade.model.CustomMessage;

/**
 * 转换链
 * @author GM
 * @date 2024-06-14
 *
 */
public class ConvertChain {

	private List<Convert> converts;
	
	public ConvertChain(List<Convert> converts) {
		this.converts = converts;
	}
	
	public CustomMessage doConvert(CustomMessage msg) {
		if (converts == null || converts.isEmpty()) {
			return msg;
		}
		for (Convert convert : converts) {
			msg = convert.doConvert(msg);
			if (msg == null) {
				break;
			}
		}
		return msg;
	}
	
}
