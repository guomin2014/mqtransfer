package com.gm.mqtransfer.worker.service.task.cluster.helix;

import org.apache.helix.NotificationContext;
import org.apache.helix.model.Message;
import org.apache.helix.participant.statemachine.StateModel;
import org.apache.helix.participant.statemachine.StateModelFactory;
import org.apache.helix.participant.statemachine.StateModelInfo;
import org.apache.helix.participant.statemachine.Transition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gm.mqtransfer.facade.model.TaskMessage;
import com.gm.mqtransfer.provider.facade.model.TopicPartitionInfo;
import com.gm.mqtransfer.provider.facade.util.PartitionUtils;
import com.gm.mqtransfer.worker.service.task.ResourceService;

/**
 * Helix State model for the Mirror Maker topic partition addition and deletion request.
 */
public class HelixWorkerOnlineOfflineStateModelFactory extends StateModelFactory<StateModel> {

	private static final Logger LOG = LoggerFactory.getLogger(HelixWorkerOnlineOfflineStateModelFactory.class);

	private String instanceId;
	
	private ResourceService resourceService;

	public HelixWorkerOnlineOfflineStateModelFactory(String instanceId, ResourceService resourceService) {
		this.instanceId = instanceId;
		this.resourceService = resourceService;
	}

	@Override
	public StateModel createNewStateModel(String partitionName) {
		return new OnlineOfflineStateModel(instanceId);
	}

	@StateModelInfo(
	      states = {"{'OFFLINE','ONLINE','DROPPED'}"},
	      initialState = "OFFLINE"
	)
	public class OnlineOfflineStateModel extends StateModel {
		private String instanceId;

		public OnlineOfflineStateModel(String instanceId) {
			this.instanceId = instanceId;
		}

		@Transition(from = "OFFLINE", to = "ONLINE")
		public void onBecomeOnlineFromOffline(Message message, NotificationContext context) {
			LOG.info("onBecomeOnlineFromOffline for partition: " + message.getPartitionName() + " to instance: " + instanceId);
			TopicPartitionInfo tp = PartitionUtils.parsePartitionKey(message.getPartitionName());
			if (tp == null) {
				LOG.error("partition is invalid, skip it-->{}", message.getPartitionName());
				return;
			}
			TaskMessage tm = new TaskMessage();
			tm.setTaskCode(message.getResourceName());
			tm.setFromPartitionKey(tp.getPartitionKey());
			resourceService.addResource(tm);
		}
		@Transition(from = "ONLINE", to = "OFFLINE")
		public void onBecomeOfflineFromOnline(Message message, NotificationContext context) {
			LOG.info("onBecomeOfflineFromOnline for partition: " + message.getPartitionName() + " to instance: " + instanceId);
			TopicPartitionInfo tp = PartitionUtils.parsePartitionKey(message.getPartitionName());
			if (tp == null) {
				LOG.error("partition is invalid, skip it-->{}", message.getPartitionName());
				return;
			}
			TaskMessage tm = new TaskMessage();
			tm.setTaskCode(message.getResourceName());
			tm.setFromPartitionKey(tp.getPartitionKey());
			resourceService.deleteResource(tm);
		}
		@Transition(from = "OFFLINE", to = "DROPPED")
		public void onBecomeDroppedFromOffline(Message message, NotificationContext context) {
			LOG.info("onBecomeDroppedFromOffline for partition: " + message.getPartitionName() + " to instance: " + instanceId);
			TopicPartitionInfo tp = PartitionUtils.parsePartitionKey(message.getPartitionName());
			if (tp == null) {
				LOG.error("partition is invalid, skip it-->{}", message.getPartitionName());
				return;
			}
			TaskMessage tm = new TaskMessage();
			tm.setTaskCode(message.getResourceName());
			tm.setFromPartitionKey(tp.getPartitionKey());
			resourceService.deleteResource(tm);
		}
	}
}
