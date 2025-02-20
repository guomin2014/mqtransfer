package com.gm.mqtransfer.facade.filter;

import com.gm.mqtransfer.facade.model.CustomMessage;

public interface Convert {
	/**
	 * 消息转换
	 * @param <T>
	 * @param task
	 * @param message
	 * @param convertMessage
	 * @return
	 */
	CustomMessage doConvert(CustomMessage message);
}
