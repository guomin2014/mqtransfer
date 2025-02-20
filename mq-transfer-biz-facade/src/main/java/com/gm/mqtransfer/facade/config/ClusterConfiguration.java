package com.gm.mqtransfer.facade.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.gm.mqtransfer.provider.facade.common.Constants;

@Configuration
@ConfigurationProperties(prefix = "transfer.cluster")
public class ClusterConfiguration {

	@Value("${cluster.mode:cluster}")
	private String mode;
	
	@Value("${cluster.tag:default}")
	private String tag;
	
	@Value("${cluster.zkUrl:}")
	private String zkUrl;
	
	private String instanceId = Constants.DEF_INSTANCE_ID_VAL;

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public String getZkUrl() {
		return zkUrl;
	}

	public void setZkUrl(String zkUrl) {
		this.zkUrl = zkUrl;
	}

	public String getInstanceId() {
		return instanceId;
	}

	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}
	
}
