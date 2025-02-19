package com.gm.mqtransfer.provider.facade.service.producer;

import com.gm.mqtransfer.provider.facade.api.producer.CheckMessageBatchRequest;
import com.gm.mqtransfer.provider.facade.api.producer.CheckMessageRequest;
import com.gm.mqtransfer.provider.facade.api.producer.CheckMessageResponse;
import com.gm.mqtransfer.provider.facade.api.producer.SendMessageAsyncRequest;
import com.gm.mqtransfer.provider.facade.api.producer.SendMessageAsyncResponse;
import com.gm.mqtransfer.provider.facade.api.producer.SendMessageRequest;
import com.gm.mqtransfer.provider.facade.api.producer.SendMessageResponse;
import com.gm.mqtransfer.provider.facade.api.producer.SubscribeRequest;
import com.gm.mqtransfer.provider.facade.api.producer.SubscribeResponse;
import com.gm.mqtransfer.provider.facade.api.producer.UnsubscribeRequest;
import com.gm.mqtransfer.provider.facade.api.producer.UnsubscribeResponse;
import com.gm.mqtransfer.provider.facade.model.ClusterInfo;
import com.gm.mqtransfer.provider.facade.model.MQMessage;

/**
 * service to producer
 * @author GuoMin
 * @date 2023-06-14 14:41:58
 *
 */
public interface ProducerService<T extends MQMessage> {
	
	public void start();
	
	public void stop();
	/**
	 * restart consumer service
	 * @param epoch
	 * @return	current epoch，normal，The returned epoch is greater than the passed in epoch
	 */
	public int restart(int epoch);
	
	public int getCurrentEpoch();
	
	public ClusterInfo getClusterInfo();
	/**
	 * Is the service ready
	 * @return
	 */
	public boolean isReady();
	
	/**
	 * Is the service shared
	 * @return
	 */
	public boolean isShare();
	/**
	 * Subscription to producer topic
	 * @param request
	 * @return
	 */
	public SubscribeResponse subscribe(SubscribeRequest request);
	
	public SubscribeResponse resubscribe(SubscribeRequest request);
	/**
	 * Unsubscribe from producer topic
	 * @param request
	 * @return
	 */
	public UnsubscribeResponse unsubscribe(UnsubscribeRequest request);
	/**
	 * Verify if the message is legal
	 * @param message
	 * @return	true: legal, false: illegal
	 */
	public CheckMessageResponse checkMessage(CheckMessageRequest<T> request);
	/**
	 * Verify if the message is legal
	 * Some message frameworks have differences in message structure when sending single messages and batch messages, 
	 * resulting in inconsistent message sizes
	 * @param message
	 * @return	true: legal, false: illegal
	 */
	public CheckMessageResponse checkMessage(CheckMessageBatchRequest<T> request);
	/**
	 * Send message in synchronous mode. 
	 * This method returns only when the sending procedure totally completes.
	 * @param task
	 * @param list
	 * @return
	 */
	public SendMessageResponse<T> sendMessage(SendMessageRequest<T> request);
	/**
	 * Send message in asynchronous mode.
	 * This method returns immediately. On sending completion, <code>sendCallback</code> will be executed.
	 * @param task
	 * @param list
	 * @param callback
	 */
	public SendMessageAsyncResponse<T> sendMessage(SendMessageAsyncRequest<T> request);

}
