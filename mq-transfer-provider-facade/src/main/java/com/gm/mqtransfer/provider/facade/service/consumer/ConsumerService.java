package com.gm.mqtransfer.provider.facade.service.consumer;

import com.gm.mqtransfer.provider.facade.api.CommitOffsetRequest;
import com.gm.mqtransfer.provider.facade.api.CommitOffsetResponse;
import com.gm.mqtransfer.provider.facade.api.FetchOffsetRangeRequest;
import com.gm.mqtransfer.provider.facade.api.FetchOffsetRangeResponse;
import com.gm.mqtransfer.provider.facade.api.FetchOffsetRequest;
import com.gm.mqtransfer.provider.facade.api.FetchOffsetResponse;
import com.gm.mqtransfer.provider.facade.api.PollMessageRequest;
import com.gm.mqtransfer.provider.facade.api.PollMessageResponse;
import com.gm.mqtransfer.provider.facade.api.SubscribeRequest;
import com.gm.mqtransfer.provider.facade.api.SubscribeResponse;
import com.gm.mqtransfer.provider.facade.api.UnsubscribeRequest;
import com.gm.mqtransfer.provider.facade.api.UnsubscribeResponse;
import com.gm.mqtransfer.provider.facade.model.ClusterInfo;

/**
 * service to consumer
 * @author GM
 * @date 2023-06-14 14:41:42
 *
 */
public interface ConsumerService {
	
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
	 * Subscription to consumer topic
	 * @param request
	 * @return
	 */
	public SubscribeResponse subscribe(SubscribeRequest request);
	
	public SubscribeResponse resubscribe(SubscribeRequest request);
	/**
	 * Unsubscribe from consumer topic
	 * @param request
	 * @return
	 */
	public UnsubscribeResponse unsubscribe(UnsubscribeRequest request);
	/**
	 * fetch the offset for consumer
	 * @param task
	 * @return
	 */
	public FetchOffsetResponse fetchOffset(FetchOffsetRequest request);
	/**
	 * fetch the offset range for partition
	 * @param request
	 * @return
	 */
	public FetchOffsetRangeResponse fetchOffsetRange(FetchOffsetRangeRequest request);
	/**
	 * commit the offset
	 * @param task		Topic and partition of tasks
	 * @param offset	The offset that need to be submitted
	 * @return			success: true, failure: false
	 */
	public CommitOffsetResponse commitOffset(CommitOffsetRequest request);
	
	/**
	 * Polling the messages
	 * @param task		Topic and partition of tasks
	 * @param maxNums	The maximum number of each pull message
	 * @param maxSize	The maximum size of each pull message, in bytes
	 * @return			The resulting PullRequest
	 */
	public PollMessageResponse pollMessage(PollMessageRequest request);
}
