package com.gm.mqtransfer.facade.model;

import java.util.HashMap;

import com.gm.mqtransfer.provider.facade.model.MQMessage;

public class CustomMessage extends MQMessage {

	private static final long serialVersionUID = -5666794308055239052L;
	/** 当前对象占用内存大小 */
	private long totalUsedMemorySize = 0;
	
	public CustomMessage(MQMessage msg) {
		this(msg, 0);
	}
	public CustomMessage(MQMessage msg, long usedMemorySize) {
		if (msg != null) {
			this.setTopic(msg.getTopic());
			this.setPartition(msg.getPartition());
			this.setHeaders(msg.getHeaders());
			this.setMessageId(msg.getMessageId());
			this.setMessageKey(msg.getMessageKey());
			this.setMessageBytes(msg.getMessageBytes());
			this.setOffset(msg.getOffset());
		}
		this.totalUsedMemorySize = usedMemorySize;
	}
	
	public long getTotalUsedMemorySize() {
		return totalUsedMemorySize;
	}

	public void setTotalUsedMemorySize(long totalUsedMemorySize) {
		this.totalUsedMemorySize = totalUsedMemorySize;
	}
	
	public void realAddHeaders(String key, String val) {
        if (headers == null) {
            synchronized (this) {
                if (headers == null) {
                    headers = new HashMap<>(5);
                }
            }
        }
        headers.put(key, val);
    }

    public String realGetHeader(String key) {
        return headers == null ? null : headers.get(key);
    }

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		builder.append("topic").append(":").append(this.getTopic()).append(",");
		builder.append("partition").append(":").append(this.getPartition()).append(",");
		builder.append("offset").append(":").append(this.getOffset()).append(",");
		builder.append("messageId").append(":").append(this.getMessageId()).append(",");
		builder.append("key").append(":").append(this.getMessageKey()).append(",");
		builder.append("header").append(":").append(this.getHeaders());
		builder.append("]");
		return builder.toString();
	}
	
}
