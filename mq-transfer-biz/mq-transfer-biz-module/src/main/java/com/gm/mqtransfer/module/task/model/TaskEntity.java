/**
* 文件：TaskEntity.java
* 版本：1.0.0
* 日期：2024-05-28
* Copyright &reg; 
* All right reserved.
*/
package com.gm.mqtransfer.module.task.model;

import com.gm.mqtransfer.facade.model.ConsumerConfig;
import com.gm.mqtransfer.facade.model.ProducerConfig;
import com.gm.mqtransfer.facade.model.TransferConfig;

/**
 * <p>Title: 任务管理</p>
 * <p>Description: 任务管理-Entity实现  </p>
 * <p>Copyright: Copyright &reg;  </p>
 * <p>Company: </p>
 * @author 
 * @version 1.0.0
 */

public class TaskEntity {

	private String name;
	/** 编码 */
	private String code;
	/** 源集群编码 */
	private String fromClusterCode;
	/** 源主题 */
	private String fromTopic;
	/** 源集群消费配置 */
	private ConsumerConfig fromConfig;
	/** 目标集群编码 */
	private String toClusterCode;
	/** 目标主题 */
	private String toTopic;
	/** 目标集群生产配置 */
	private ProducerConfig toConfig;
	/** 转发配置 */
	private TransferConfig transferConfig;
	/** 告警配置 */
	private String alarmConfig;
	/** 状态，0：停用，1：启用 */
	private Integer status;
	
	/**
	 * 获取 名称
	 * @return String
	 */
	public String getName(){
		return this.name;
	}

	/**
	 * 设置 名称
	 * @param name
	 */
	public void setName(String name){
		this.name = name;
	}

	/**
	 * 获取 编码
	 * @return String
	 */
	public String getCode(){
		return this.code;
	}

	/**
	 * 设置 编码
	 * @param code
	 */
	public void setCode(String code){
		this.code = code;
	}

	/**
	 * 获取 源集群编码
	 * @return String
	 */
	public String getFromClusterCode(){
		return this.fromClusterCode;
	}

	/**
	 * 设置 源集群编码
	 * @param fromClusterCode
	 */
	public void setFromClusterCode(String fromClusterCode){
		this.fromClusterCode = fromClusterCode;
	}

	/**
	 * 获取 源主题
	 * @return String
	 */
	public String getFromTopic(){
		return this.fromTopic;
	}

	/**
	 * 设置 源主题
	 * @param fromTopic
	 */
	public void setFromTopic(String fromTopic){
		this.fromTopic = fromTopic;
	}

	/**
	 * 获取 源集群消费配置
	 * @return String
	 */
	public ConsumerConfig getFromConfig(){
		return this.fromConfig;
	}

	/**
	 * 设置 源集群消费配置
	 * @param fromConfig
	 */
	public void setFromConfig(ConsumerConfig fromConfig){
		this.fromConfig = fromConfig;
	}

	/**
	 * 获取 目标集群编码
	 * @return String
	 */
	public String getToClusterCode(){
		return this.toClusterCode;
	}

	/**
	 * 设置 目标集群编码
	 * @param toClusterCode
	 */
	public void setToClusterCode(String toClusterCode){
		this.toClusterCode = toClusterCode;
	}

	/**
	 * 获取 目标主题
	 * @return String
	 */
	public String getToTopic(){
		return this.toTopic;
	}

	/**
	 * 设置 目标主题
	 * @param toTopic
	 */
	public void setToTopic(String toTopic){
		this.toTopic = toTopic;
	}

	/**
	 * 获取 目标集群生产配置
	 * @return String
	 */
	public ProducerConfig getToConfig(){
		return this.toConfig;
	}

	/**
	 * 设置 目标集群生产配置
	 * @param toConfig
	 */
	public void setToConfig(ProducerConfig toConfig){
		this.toConfig = toConfig;
	}

	public TransferConfig getTransferConfig() {
		return transferConfig;
	}

	public void setTransferConfig(TransferConfig transferConfig) {
		this.transferConfig = transferConfig;
	}

	public String getAlarmConfig() {
		return alarmConfig;
	}

	public void setAlarmConfig(String alarmConfig) {
		this.alarmConfig = alarmConfig;
	}

	/**
	 * 获取 状态，0：停用，1：启用
	 * @return Integer
	 */
	public Integer getStatus(){
		return this.status;
	}

	/**
	 * 设置 状态，0：停用，1：启用
	 * @param status
	 */
	public void setStatus(Integer status){
		this.status = status;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder("");
		sb.append(",name:").append(getName())
		  .append(",code:").append(getCode())
		  .append(",fromClusterCode:").append(getFromClusterCode())
		  .append(",fromTopic:").append(getFromTopic())
		  .append(",fromConfig:").append(getFromConfig())
		  .append(",toClusterCode:").append(getToClusterCode())
		  .append(",toTopic:").append(getToTopic())
		  .append(",toConfig:").append(getToConfig())
		  .append(",transferConfig:").append(getTransferConfig())
		  .append(",alarmConfig:").append(getAlarmConfig())
		  .append(",status:").append(getStatus());
		return sb.toString();
	}

}