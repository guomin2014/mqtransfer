package com.gm.mqtransfer.worker.service.task.cluster.helix;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.helix.NotificationContext;
import org.apache.helix.messaging.handling.HelixTaskResult;
import org.apache.helix.messaging.handling.MessageHandler;
import org.apache.helix.messaging.handling.MultiTypeMessageHandlerFactory;
import org.apache.helix.model.Message;
import org.apache.helix.model.Message.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.gm.mqtransfer.facade.common.TaskMessageType;
import com.gm.mqtransfer.module.task.model.TaskMigrationResult;
import com.gm.mqtransfer.module.task.service.TaskService;
import com.gm.mqtransfer.worker.model.Task;
import com.gm.mqtransfer.worker.model.TaskSharding;
import com.gm.mqtransfer.worker.service.task.TaskShardingRunningService;
import com.google.common.collect.ImmutableList;

/**
 * 自定义消息处理器，详见：org.apache.helix.examples.BootstrapProcess
 * @author GM
 * @date 2022-06-23
 */
@Component
public class CustomMessageHandlerFactory implements MultiTypeMessageHandlerFactory {
	
	private static final Logger logger = LoggerFactory.getLogger(CustomMessageHandlerFactory.class);
	@Autowired
	private TaskShardingRunningService taskShardingRunningService;
	@Autowired
	private TaskService taskService;
	public CustomMessageHandlerFactory() {
	}

	@Override
	public MessageHandler createHandler(Message message, NotificationContext context) {

		return new CustomMessageHandler(message, context);
	}

	@Override
	public List<String> getMessageTypes() {
		return ImmutableList.of(MessageType.USER_DEFINE_MSG.name());
	}

	@Override
	public void reset() {

	}

	class CustomMessageHandler extends MessageHandler {

		public CustomMessageHandler(Message message, NotificationContext context) {
			super(message, context);
		}

		@Override
		public HelixTaskResult handleMessage() throws InterruptedException {
			logger.info("receive message-->msgId:{}, msgSubType:{}", _message.getMsgId(), _message.getMsgSubType());
			HelixTaskResult result = new HelixTaskResult();
			try {
				String msgSubType = _message.getMsgSubType();
				if (msgSubType.equalsIgnoreCase(TaskMessageType.RESOURCE_MIGRATION.name())) {
					String resourceName = _message.getResourceName();
					List<String> partitionNames = _message.getPartitionNames();
					result.getTaskResultMap().put("data", this.handlerResourceMigration(resourceName, partitionNames));
					result.setSuccess(true);
				} else if (msgSubType.equalsIgnoreCase(TaskMessageType.REQUEST_RESOURCE_STATUS.name())) {
					String resourceName = _message.getResourceName();
					result.getTaskResultMap().put("data", this.handlerResourceStatus(resourceName));
					result.setSuccess(true);
				} else if (msgSubType.equalsIgnoreCase(TaskMessageType.REQUEST_ALL_RESOURCE_STATUS.name())) {
					result.getTaskResultMap().put("data", this.handlerAllResourceStatus());
					result.setSuccess(true);
				} else {
					result.setSuccess(false);
					result.setMessage("Illegal msgsubtype[" + msgSubType + "]");
				}
			} catch (Exception e) {
				result.setSuccess(false);
				String msg = e.getMessage();
				result.setMessage(msg != null && msg.length() > 3000 ? msg.substring(0, 3000) : msg);
				logger.error("handler message failure-->msgId:" + _message.getMsgId() + ", msgSubType:" + _message.getMsgSubType(), e);
			}
			return result;
		}

		@Override
		public void onError(Exception e, ErrorCode code, ErrorType type) {
			e.printStackTrace();
		}
		
		private String handlerResourceMigration(String resourceName, List<String> partitionNames) {
			Set<String> existsPartition = new HashSet<>();
			Task task = taskShardingRunningService.getTask(resourceName);
			if (task != null) {
				Map<String, TaskSharding> shardingMap = task.getTaskShardingMap();
				if (shardingMap != null) {
					for (String partition : partitionNames) {
						if (shardingMap.containsKey(partition)) {
							existsPartition.add(partition);
						}
					}
				}
			}
			JSONObject json = new JSONObject(true);
			for (String partition : existsPartition) {
				boolean updateResult = taskService.markPartitionMigration(resourceName, partition);
				json.put(partition, updateResult ? TaskMigrationResult.SUCCESS.getValue() : TaskMigrationResult.NOTLOCK.getValue());
			}
			for (String partition : partitionNames) {
				if (existsPartition.contains(partition)) {
					continue;
				}
				json.put(partition, TaskMigrationResult.NOTEXISTS.getValue());
			}
			return json.toJSONString();
		}
		
		private String handlerResourceStatus(String resourceName) {
			JSONObject json = new JSONObject(true);
//			//获取统计信息
//			long maxAvailableMemory = this._controller.getStatService().getMaxAvaiableMemoryForTask(resourceName);
//			long totalUsedMemory = this._controller.getStatService().getUsedMemoryForTask(resourceName);
//			long totalConsumerCount = this._controller.getStatService().getTotalConsumerMsgCountForTask(resourceName);
//			long totalFilterCount = this._controller.getStatService().getTotalFilterMsgCountForTask(resourceName);
//			long totalProducerCount = this._controller.getStatService().getTotalProducerMsgCountForTask(resourceName);
//			long totalConsumerLag = 0;
//			long totalProducerLag = 0;
//			//获取缓存队列信息 
//			Map<String, Integer> queueMap = this._controller.getDataCacheService().countQueueGroupById(resourceName);
//			int totalCacheQueueCount = 0;
//			if (queueMap != null) {
//				for(Map.Entry<String, Integer> entry : queueMap.entrySet()) {
//					if (entry.getValue() != null) {
//						totalCacheQueueCount += entry.getValue();
//					}
//				}
//			}
//			Map<String, JSONObject> partitionInfoMap = new TreeMap<>(new Comparator<String>() {
//				@Override
//				public int compare(String o1, String o2) {
//					if (StringUtils.isBlank(o1)) {
//						return 1;
//					}
//					if (StringUtils.isBlank(o2)) {
//						return -1;
//					}
//					//brokerName+分区ID
//					String[] s1 = o1.split("#");
//					String[] s2 = o2.split("#");
//					if (s1.length == s2.length) {
//						String bn1 = s1[0] != null ? s1[0] : "";
//						String bn2 = s2[0] != null ? s2[0] : "";
//						if (s1.length == 1) {//只有数字
//							try {
//								return Integer.parseInt(bn1) > Integer.parseInt(bn2) ? 1 : -1;
//							} catch (Exception e) {
//								return bn1.compareTo(bn2);
//							}
//						} else {
//							int ct = bn1.compareTo(bn2);
//							if (ct == 0 && s1.length >= 2) {
//								String p1 = s1[1] != null ? s1[1] : "";
//								String p2 = s2[1] != null ? s2[1] : "";
//								try {
//									return Integer.parseInt(p1) > Integer.parseInt(p2) ? 1 : -1;
//								} catch (Exception e) {
//									return p1.compareTo(p2);
//								}
//							} else {
//								return ct;
//							}
//						}
//					} else {
//						return o1.compareTo(o2);
//					}
//				}
//			});
//			Map<String, TaskPartition> partitionTaskMap = this._controller.getTaskService().getPartitionTask(resourceName);
//			if (partitionTaskMap != null) {
//				for(Map.Entry<String, TaskPartition> entry : partitionTaskMap.entrySet()) {
//					//获取消费端的当前消费位置点、已提交位置点、最大位置点、堆积量、最后一次消费时间
//					TaskPartition taskInfo = entry.getValue();
//					Long firstOffset = taskInfo.getFirstOffset();
//					Long startOffset = taskInfo.getStartOffset();
//					Long startOffsetLastTime = taskInfo.getStartOffsetLastTime();
//					Long maxOffset = taskInfo.getHighWatermark();
//					Long commitOffset = taskInfo.getCommitOffset();
//					Long commitOffsetLastTime = taskInfo.getCommitOffsetLastTime();
//					String targetPartition = PartitionUtils.generatorPartitionKey(taskInfo.getToBrokerName(), taskInfo.getToPartition());
//					Integer cacheQueueSize = queueMap.get(entry.getKey());
//					JSONObject partitionJson = partitionInfoMap.get(entry.getKey());
//					if (partitionJson == null) {
//						partitionJson = new JSONObject(true);
//						partitionInfoMap.put(entry.getKey(), partitionJson);
//					}
//					if (maxOffset != null && startOffset != null) {
//						totalConsumerLag += maxOffset - startOffset;
//					}
//					if (maxOffset != null && commitOffset != null) {
//						totalProducerLag += maxOffset - commitOffset;
//					}
//					partitionJson.put("源分区", entry.getKey());
//					partitionJson.put("目标分区", targetPartition);
//					partitionJson.put("开始位置点", firstOffset != null ? firstOffset : "无");
//					partitionJson.put("最大位置点", maxOffset != null ? maxOffset : "无");
//					partitionJson.put("消费位置点", startOffset != null ? startOffset : "无");
//					partitionJson.put("消费Lag", maxOffset != null && startOffset != null ? maxOffset - startOffset : "无");
//					partitionJson.put("提交位置点", commitOffset != null ? commitOffset : "无");
//					partitionJson.put("生产Lag", maxOffset != null && commitOffset != null ? maxOffset - commitOffset : "无");
//					partitionJson.put("最后消费时间", startOffsetLastTime != null ? DateUtils.getDateTime(new Date(startOffsetLastTime), DateUtils.P_yyyy_MM_dd_HH_mm_ss) : "无");
//					partitionJson.put("最后提交时间", commitOffsetLastTime != null ? DateUtils.getDateTime(new Date(commitOffsetLastTime), DateUtils.P_yyyy_MM_dd_HH_mm_ss) : "无");
//					partitionJson.put("缓存队列数", cacheQueueSize != null ? cacheQueueSize : 0);
//				}
//			}
//			json.put("总可用内存", ByteUtil.formatMaxUnit(maxAvailableMemory));
//			json.put("总已用内存", ByteUtil.formatMaxUnit(totalUsedMemory));
//			json.put("总消费记录数", totalConsumerCount);
//			json.put("总过滤记录数", totalFilterCount);
//			json.put("总生产记录数", totalProducerCount);
//			json.put("总缓存记录数", totalCacheQueueCount);
//			json.put("总消费延迟数", totalConsumerLag);
//			json.put("总生产延迟数", totalProducerLag);
//			json.put("分区信息", partitionInfoMap.values());
			return json.toJSONString();
		}
		/**
		 * 获取所有资源统计信息
		 * @return
		 */
		private String handlerAllResourceStatus() {
			JSONObject json = new JSONObject(true);
//			long totalMemory = this._controller.getStatService().getTotalMemory();
//			long totalAvaiableMemory = this._controller.getStatService().getTotalAvaiableMemory();
//			long totalUsedMemory = this._controller.getStatService().getTotalUsedMemory();
//			int consumerCountRate = this._controller.getStatService().getConsumerCountRate();
//			long consumerSizeRate = this._controller.getStatService().getConsumerSizeRate();
//			int producerCountRate = this._controller.getStatService().getProducerCountRate();
//			long producerSizeRate = this._controller.getStatService().getProducerSizeRate();
//			int totalCacheQueueSize = this._controller.getDataCacheService().countAllQueue();
//			json.put("jvmTotalMemory", totalMemory);//实例总内存
//			json.put("queueTotalAvailableMemory", totalAvaiableMemory);//缓存总可用内存
//			json.put("queueTotalUsedMemory", totalUsedMemory);//缓存已使用内存
//			json.put("queueTotalCacheSize", totalCacheQueueSize);//缓存队列数
//			json.put("consumerCountRate", consumerCountRate);//消费速率，单位：条/秒
//			json.put("consumerSizeRate", consumerSizeRate);//消费速率，单位：byte/秒
//			json.put("producerCountRate", producerCountRate);//生产速率，单位：条/秒
//			json.put("producerSizeRate", producerSizeRate);//生产速率，单位：byte/秒
			return json.toJSONString();
		}
	}
}
