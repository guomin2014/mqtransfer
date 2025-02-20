package com.gm.mqtransfer.facade.service.sharding;

import com.gm.mqtransfer.facade.model.TaskMessage;

public interface ShardingService {

	/**
	 * add task sharding
	 * @param message
	 */
	void addSharding(TaskMessage message);
	/**
	 * delete task sharding
	 * @param message
	 */
	void deleteSharding(TaskMessage message);
}
