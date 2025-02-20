package com.gm.mqtransfer.facade.filter;

import com.gm.mqtransfer.facade.model.CustomMessage;

/**
 * 消息大小过滤器
 * @author GM
 *
 */
public class SizeFilter implements Filter {

	private long maxSize;
	
	public SizeFilter(long maxSize) {
		this.maxSize = maxSize;
	}
	@Override
	public boolean doFilter(CustomMessage msg) {
		long msgLen = msg.getMessageBytes() != null ? msg.getMessageBytes().length : 0;
		return msgLen > maxSize;
	}

}
