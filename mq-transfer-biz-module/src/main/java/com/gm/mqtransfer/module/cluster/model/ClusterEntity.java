/**
* 文件：ClusterEntity.java
* 版本：1.0.0
* 日期：2024-05-28
* Copyright &reg; 
* All right reserved.
*/
package com.gm.mqtransfer.module.cluster.model;

/**
 * <p>Title: 集群管理</p>
 * <p>Description: 集群管理-Entity实现  </p>
 * <p>Copyright: Copyright &reg;  </p>
 * <p>Company: </p>
 * @author 
 * @version 1.0.0
 */

public class ClusterEntity {

	/** 集群名称 */
	private String name;
	/** 集群编码 */
	private String code;
	/** 集群类型，如：kafka,rocketmq */
	private String type;
	/** 版本号 */
	private String version;
	/** 客户端版本号，多个使用逗号分隔 */
	private String clientVersion;
	/** zk地址 */
	private String zkUrl;
	/** 群集nameserver地址 */
	private String nameSvrAddr;
	/** 主机列表 */
	private String brokerList;
	/** 状态，0：停用，1：启用 */
	private Integer status;

	/**
	 * 获取 集群名称
	 * @return String
	 */
	public String getName(){
		return this.name;
	}

	/**
	 * 设置 集群名称
	 * @param name
	 */
	public void setName(String name){
		this.name = name;
	}

	/**
	 * 获取 集群编码
	 * @return String
	 */
	public String getCode(){
		return this.code;
	}

	/**
	 * 设置 集群编码
	 * @param code
	 */
	public void setCode(String code){
		this.code = code;
	}

	/**
	 * 获取 集群类型，如：kafka,rocketmq
	 * @return String
	 */
	public String getType(){
		return this.type;
	}

	/**
	 * 设置 集群类型，如：kafka,rocketmq
	 * @param type
	 */
	public void setType(String type){
		this.type = type;
	}

	/**
	 * 获取 版本号
	 * @return String
	 */
	public String getVersion(){
		return this.version;
	}

	/**
	 * 设置 版本号
	 * @param version
	 */
	public void setVersion(String version){
		this.version = version;
	}

	/**
	 * 获取 客户端版本号，多个使用逗号分隔
	 * @return String
	 */
	public String getClientVersion(){
		return this.clientVersion;
	}

	/**
	 * 设置 客户端版本号，多个使用逗号分隔
	 * @param clientVersion
	 */
	public void setClientVersion(String clientVersion){
		this.clientVersion = clientVersion;
	}

	/**
	 * 获取 zk地址
	 * @return String
	 */
	public String getZkUrl(){
		return this.zkUrl;
	}

	/**
	 * 设置 zk地址
	 * @param zkUrl
	 */
	public void setZkUrl(String zkUrl){
		this.zkUrl = zkUrl;
	}

	public String getNameSvrAddr() {
		return nameSvrAddr;
	}

	public void setNameSvrAddr(String nameSvrAddr) {
		this.nameSvrAddr = nameSvrAddr;
	}

	/**
	 * 获取 主机列表
	 * @return String
	 */
	public String getBrokerList(){
		return this.brokerList;
	}

	/**
	 * 设置 主机列表
	 * @param brokerList
	 */
	public void setBrokerList(String brokerList){
		this.brokerList = brokerList;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder("");
		sb.append(",name:").append(getName())
		  .append(",code:").append(getCode())
		  .append(",type:").append(getType())
		  .append(",version:").append(getVersion())
		  .append(",clientVersion:").append(getClientVersion())
		  .append(",zkUrl:").append(getZkUrl())
		  .append(",nameSvrAddr:").append(getNameSvrAddr())
		  .append(",brokerList:").append(getBrokerList())
		  .append(",status:").append(getStatus());
		return sb.toString();
	}

}