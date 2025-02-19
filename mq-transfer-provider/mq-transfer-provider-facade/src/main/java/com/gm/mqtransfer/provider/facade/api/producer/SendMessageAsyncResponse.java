package com.gm.mqtransfer.provider.facade.api.producer;

import com.gm.mqtransfer.provider.facade.api.Response;
import com.gm.mqtransfer.provider.facade.model.MQMessage;

public class SendMessageAsyncResponse<T extends MQMessage> extends Response<SendMessageResult<T>> {

}
