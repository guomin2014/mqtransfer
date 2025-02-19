package com.gm.mqtransfer.manager.support.helix;

import com.alibaba.fastjson.JSONObject;

public class TopicInfo {
	private final String _topic;
	private final int _partitionNum;

	public TopicInfo(String topic, int numPartitions) {
		_topic = topic;
		_partitionNum = numPartitions;
	}

	public int getPartitionNum() {
		return _partitionNum;
	}

	public String getTopic() {
		return _topic;
	}

	public String toString() {
		return String.format("{topic: %s, _partitionNum: %s}", _topic, _partitionNum);
	}

	public JSONObject toJSON() {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("topic", _topic);
		jsonObject.put("numPartitions", _partitionNum);
		return jsonObject;
	}
}

