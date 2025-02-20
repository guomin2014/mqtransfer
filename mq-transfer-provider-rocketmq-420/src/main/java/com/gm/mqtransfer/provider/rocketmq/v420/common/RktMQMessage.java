package com.gm.mqtransfer.provider.rocketmq.v420.common;

import java.util.Map;

import com.gm.mqtransfer.provider.facade.model.MQMessage;

public class RktMQMessage extends MQMessage {

	private static final long serialVersionUID = 6742522346544688743L;
	
	private long sentTimestamp;
    private int reconsumeTimes;
    /**
     * If a message transferred between different cluster,
     * it will lost origin message id generate by ingress cluster
     * and it's a bother when we want to trace a message
     */
    private String storeId;
    private String tags;

    /**
     * 由于老系统的消息是plain的格式,
     * 新老系统并行阶段会，新/老消息格式会导致老/新SDK消费异常。
     * 暂未实现。{@link Deprecated}
     */
    private Map<String, String> props;

    /**
     * 生产端可见
     * 延迟消息，参考：org.apache.rocketmq.store.config.MessageStoreConfig
     * 1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
     *        |
     *     level-3
     */
    private int delayTimeLevel;
	
	public RktMQMessage(String topic, String partition, String messageId, String messageKey, byte[] messageBytes) {
		super(topic, partition, messageId, messageKey, messageBytes);
	}

	public long getSentTimestamp() {
		return sentTimestamp;
	}

	public void setSentTimestamp(long sentTimestamp) {
		this.sentTimestamp = sentTimestamp;
	}

	public int getReconsumeTimes() {
		return reconsumeTimes;
	}

	public void setReconsumeTimes(int reconsumeTimes) {
		this.reconsumeTimes = reconsumeTimes;
	}

	public String getStoreId() {
		return storeId;
	}

	public void setStoreId(String storeId) {
		this.storeId = storeId;
	}

	public String getTags() {
		return tags;
	}

	public void setTags(String tags) {
		this.tags = tags;
	}

	public Map<String, String> getProps() {
		return props;
	}

	public void setProps(Map<String, String> props) {
		this.props = props;
	}

	public int getDelayTimeLevel() {
		return delayTimeLevel;
	}

	public void setDelayTimeLevel(int delayTimeLevel) {
		this.delayTimeLevel = delayTimeLevel;
	}

}
