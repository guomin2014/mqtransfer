package com.gm.mqtransfer.worker.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.gm.mqtransfer.facade.model.CustomMessage;
import com.gm.mqtransfer.provider.facade.service.consumer.ConsumerService;
import com.gm.mqtransfer.provider.facade.service.producer.ProducerService;

public class Task {

	/** Consumer services during sharing */
	private ConsumerService consumer;
	/** Producer services during sharing */
	private ProducerService<CustomMessage> producer;
	
	private Map<String, TaskSharding> taskShardingMap = new ConcurrentHashMap<>();
	
	public ConsumerService getConsumer() {
		return consumer;
	}
	public void setConsumer(ConsumerService consumer) {
		this.consumer = consumer;
	}
	public ProducerService<CustomMessage> getProducer() {
		return producer;
	}
	public void setProducer(ProducerService<CustomMessage> producer) {
		this.producer = producer;
	}
	public Map<String, TaskSharding> getTaskShardingMap() {
		return taskShardingMap;
	}
	public void setTaskShardingMap(Map<String, TaskSharding> taskShardingMap) {
		this.taskShardingMap = taskShardingMap;
	}
	public boolean existsTaskSharding(String partitionKey) {
		return taskShardingMap.containsKey(partitionKey);
	}
	
	public boolean existsTaskSharding() {
		return !taskShardingMap.isEmpty();
	}
	
	public void addTaskSharding(String partitionKey, TaskSharding taskSharding) {
		taskShardingMap.put(partitionKey, taskSharding);
	}
	
	public TaskSharding removeTaskSharding(String partitionKey) {
		return taskShardingMap.remove(partitionKey);
	}
	
}
