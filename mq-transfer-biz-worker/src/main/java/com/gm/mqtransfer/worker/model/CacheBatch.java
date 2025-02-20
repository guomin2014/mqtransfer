package com.gm.mqtransfer.worker.model;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

import com.gm.mqtransfer.facade.model.CustomMessage;

public class CacheBatch {

	/** 消息数量 */
	private int messageCount;
	/** 消息大小 */
	private long messageSize;
	/** 消息队列 */
	private Deque<CustomMessage> messages = new ConcurrentLinkedDeque<>();
	
	public int getMessageCount() {
		return messageCount;
	}
	public void setMessageCount(int messageCount) {
		this.messageCount = messageCount;
	}
	public long getMessageSize() {
		return messageSize;
	}
	public void setMessageSize(long messageSize) {
		this.messageSize = messageSize;
	}
	public Deque<CustomMessage> getMessages() {
		return messages;
	}
	public void setMessages(Deque<CustomMessage> messages) {
		this.messages = messages;
	}
	
	/**
	 * 将消息放入队列尾部
	 * @param messageList
	 */
	public void put(List<CustomMessage> messageList) {
		if (messageList == null || messageList.isEmpty()) {
			return;
		}
		long messageSize = 0;
		for (CustomMessage message : messageList) {
			messageSize += message.getTotalUsedMemorySize();
		}
		synchronized (messages) {
			this.messages.addAll(messageList);
			this.messageCount += messageList.size();
			this.messageSize += messageSize;
		}
	}
	/**
	 * 将消息放入队列头部
	 * @param messageList
	 */
	public void putHead(List<CustomMessage> messageList) {
		if (messageList == null || messageList.isEmpty()) {
			return;
		}
		long messageSize = 0;
		for (CustomMessage message : messageList) {
			messageSize += message.getTotalUsedMemorySize();
		}
		synchronized (messages) {
			//按放入列表的顺序直接添加到缓存队列的头部
			for (int i = messageList.size() - 1; i >= 0; i--) {
				CustomMessage msg = messageList.get(i);
				this.messages.addFirst(msg);
			}
			this.messageCount += messageList.size();
			this.messageSize += messageSize;
		}
	}
	/**
	 * 从队列头部获取指定记录数消息
	 * @param maxSize
	 * @return
	 */
	public List<CustomMessage> poll(int maxSize) {
		List<CustomMessage> retList = new ArrayList<>();
		long messageSize = 0;
		synchronized (messages) {
			try {
				while(retList.size() < maxSize && !messages.isEmpty()) {
					CustomMessage message = messages.poll();
					if (message != null) {
						messageSize += message.getTotalUsedMemorySize();
						retList.add(message);
					}
				}
			} catch (Exception e) {}
			this.messageCount -= retList.size();
			this.messageSize -= messageSize;
		}
		return retList;
	}
	
	public CustomMessage poll() {
		List<CustomMessage> retList = this.poll(1);
		if (retList.isEmpty()) {
			return null;
		}
		return retList.get(0);
	}
	
	public List<CustomMessage> cleanQueue() {
		List<CustomMessage> retList = new ArrayList<>();
		long messageSize = 0;
		synchronized (messages) {
			try {
				while(!messages.isEmpty()) {
					CustomMessage message = messages.poll();
					if (message != null) {
						messageSize += message.getTotalUsedMemorySize();
						retList.add(message);
					}
				}
			} catch (Exception e) {}
			this.messageCount -= retList.size();
			this.messageSize -= messageSize;
		}
		return retList;
	}
	
}
