/**
* 文件：TaskService.java
* 版本：1.0.0
* 日期：2024-05-28
* Copyright &reg; 
* All right reserved.
*/
package com.gm.mqtransfer.module.task.service;

import java.util.List;

import com.gm.mqtransfer.facade.model.TaskPartitionStatInfo;
import com.gm.mqtransfer.facade.model.TaskStatInfo;
import com.gm.mqtransfer.module.task.listener.TaskChangeListener;
import com.gm.mqtransfer.module.task.model.TaskEntity;
import com.gm.mqtransfer.module.task.model.TaskMigrationStatus;
import com.gm.mqtransfer.provider.facade.model.TopicPartitionMapping;

/**
 * <p>Title: 任务管理</p>
 * <p>Description: 任务管理-Service接口  </p>
 * <p>Copyright: Copyright &reg;  </p>
 * <p>Company: </p>
 * @author 
 * @version 1.0.0
 */

public interface TaskService {

	/**
	 * 查询所有可用任务
	 * @return
	 */
	List<TaskEntity> findAllEnable();
	/**
	 * 查询所有任务（包含停用的任务）
	 * @return
	 */
	List<TaskEntity> findAll();
	/**
	 * 获取指定任务
	 * @param code
	 * @return
	 */
	TaskEntity get(String code);
	/**
	 * 保存任务（修改任务时：编码、源集群、源主题、目标集群、目标主题不能修改，其它字段均可修改）
	 * @param entity
	 */
	void save(TaskEntity entity);
	void delete(String code);
	/**
	 * 更新任务状态
	 * @param code
	 * @param status
	 */
	void updateTaskStatus(String code, Integer status);
	/**
	 * 获取任务统计信息
	 * @param entity
	 */
	TaskStatInfo getStatInfo(String code);
	/**
	 * 更新任务分区分配信息
	 * @param entity
	 * @param allocateList
	 */
	void updatePartitionMapping(TaskEntity entity, List<TopicPartitionMapping> allocateList);
	/**
	 * 获取任务分区分配信息
	 * @param entity
	 * @return
	 */
	List<TopicPartitionMapping> getPartitionMapping(String code);
	/**
	 * 获取任务指定分区分配信息
	 * @param code
	 * @param fromPartitionKey
	 * @return
	 */
	TopicPartitionMapping getPartitionMapping(String code, String fromPartitionKey);
	/**
	 * 刷新任务的内存使用情况
	 * @param code
	 * @param maxAvailableMemory
	 * @param totalUsedMemory
	 */
	void refreshMemoryStat(String code, Long maxAvailableMemory, Long totalUsedMemory);
	/**
	 * 更新任务消费统计信息
	 * @param code
	 * @param data
	 */
	void refreshConsumerStat(String code, TaskPartitionStatInfo data);
	/**
	 * 更新任务生产统计信息
	 * @param code
	 * @param data
	 */
	void refreshProducerStat(String code, TaskPartitionStatInfo data);
	/**
	 * 添加任务变更监控
	 * @param listener
	 */
	void addListener(TaskChangeListener listener);
	/**
	 * 监控任务分片迁移
	 * @param code
	 * @param partitionKey
	 * @param originalInstanceName 原始执行worker实例
	 * @param newlInstanceName		新的执行worker实例
	 */
	void watchPartitionMigration(String code, String partitionKey, String originalInstanceName, String newlInstanceName);
	/**
	 * 标识分片迁移
	 * @param code
	 * @param partitionKey
	 * @return
	 */
	boolean markPartitionMigration(String code, String partitionKey);
	/**
	 * 完成任务分片迁移
	 * @param code
	 * @param partitionKey
	 */
	void tryCompletePartitionMigration(String code, String partitionKey);
	/**
	 * 更新任务分片迁移状态
	 * @param code
	 * @param partitionKey
	 * @param status
	 */
	boolean updatePartitionMigration(String code, String partitionKey, TaskMigrationStatus status);
	
	TaskMigrationStatus getPartitionMigrationStatus(String code, String partitionKey);
}