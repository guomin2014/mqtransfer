package com.gm.mqtransfer.provider.facade.model;

import java.util.Map;

/**
 * 
 * @author GM
 * @date 2023-06-14 15:18:25
 *
 */
public class MQMessage extends BaseMessage {

	private static final long serialVersionUID = -4705052389417590448L;

	/** topic */
    protected String topic;
    /** Partition ID */
    protected String partition;
    /** Offset of message */
    protected Long offset;
	/** header of message */
    protected Map<String, String> headers;
    /** Key of message */
    protected String messageKey;
    /** Id of message */
    protected String messageId;
    /** content */
    protected byte[] messageBytes;
    
    public MQMessage() {
    	
    }
    public MQMessage(String topic, String partition, String messageId, String messageKey, byte[] messageBytes) {
    	this.topic = topic;
    	this.partition = partition;
    	this.messageId = messageId;
    	this.messageKey = messageKey;
    	this.messageBytes = messageBytes;
    }
    
	public String getTopic() {
		return topic;
	}
	public void setTopic(String topic) {
		this.topic = topic;
	}
	public String getPartition() {
		return partition;
	}
	public void setPartition(String partition) {
		this.partition = partition;
	}
	public Long getOffset() {
		return offset;
	}
	public void setOffset(Long offset) {
		this.offset = offset;
	}
	public Map<String, String> getHeaders() {
		return headers;
	}
	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}
	public String getMessageKey() {
		return messageKey;
	}
	public void setMessageKey(String messageKey) {
		this.messageKey = messageKey;
	}
	public String getMessageId() {
		return messageId;
	}
	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}
	public byte[] getMessageBytes() {
		return messageBytes;
	}
	public void setMessageBytes(byte[] messageBytes) {
		this.messageBytes = messageBytes;
	}
	@Override
	public String toString() {
		return "MQMessage [topic=" + topic + ", partition=" + partition + ", offset=" + offset + ", message="
				+ new String(messageBytes) + "]";
	}
	
	
}
