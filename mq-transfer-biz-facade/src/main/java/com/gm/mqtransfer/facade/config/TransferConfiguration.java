package com.gm.mqtransfer.facade.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.gm.mqtransfer.facade.model.TransferCluster;
import com.gm.mqtransfer.facade.model.TransferTask;

@Configuration
@ConfigurationProperties(prefix = "transfer")
public class TransferConfiguration {

	private List<TransferCluster> clusterList;
	
	private List<TransferTask> taskList;
	
	private Map<String, TransferCluster> clusterMap = new HashMap<>();
	private Map<String, TransferTask> taskMap = new HashMap<>();

	public List<TransferCluster> getClusterList() {
		return clusterList;
	}

	public void setClusterList(List<TransferCluster> clusterList) {
		this.clusterList = clusterList;
		this.clusterMap.clear();
		if (clusterList != null) {
			for (TransferCluster cluster : clusterList) {
				clusterMap.put(cluster.getCode(), cluster);
			}
		}
	}

	public List<TransferTask> getTaskList() {
		return taskList;
	}

	public void setTaskList(List<TransferTask> taskList) {
		this.taskList = taskList;
		this.taskMap.clear();
		if (taskList != null) {
			for (TransferTask task : taskList) {
				taskMap.put(task.getCode(), task);
			}
		}
	}
	
	public TransferCluster getClusterByCode(String clusterCode) {
		return clusterMap.get(clusterCode);
	}
	public TransferTask getTaskByCode(String taskCode) {
		return taskMap.get(taskCode);
	}
	
}
