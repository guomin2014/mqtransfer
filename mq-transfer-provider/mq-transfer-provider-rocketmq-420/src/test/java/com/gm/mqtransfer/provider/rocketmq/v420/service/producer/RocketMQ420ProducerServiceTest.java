package com.gm.mqtransfer.provider.rocketmq.v420.service.producer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.gm.mqtransfer.provider.facade.api.producer.CheckMessageBatchRequest;
import com.gm.mqtransfer.provider.facade.api.producer.CheckMessageResponse;
import com.gm.mqtransfer.provider.facade.api.producer.SendMessageRequest;
import com.gm.mqtransfer.provider.facade.api.producer.SendMessageResponse;
import com.gm.mqtransfer.provider.facade.model.ProducerClientConfig;
import com.gm.mqtransfer.provider.facade.model.ProducerPartition;
import com.gm.mqtransfer.provider.facade.model.RocketMQClusterInfo;
import com.gm.mqtransfer.provider.facade.util.PartitionUtils;
import com.gm.mqtransfer.provider.rocketmq.v420.common.RktMQMessage;

public class RocketMQ420ProducerServiceTest {

	static RocketMQ420ProducerService<RktMQMessage> producerService;
	static String taskCode = "task1";
	static String toClusterCode = "bigdata";
	static String toTopic = "rkt-source-01";
	static int toPartition = 1;
	static String toBrokerName = "broker-a";
	static String expectStartOffset = null;
	static Long expectStartOffsetValue = null;
	@BeforeAll
	public static void init() {
		ProducerClientConfig config = new ProducerClientConfig();
		config.setClientShare(true);
		config.setInstanceIndex(1);
		config.setInstanceCode(taskCode);
		config.setProducerProps(new Properties());
		RocketMQClusterInfo cluster = new RocketMQClusterInfo();
		cluster.setCode(toClusterCode);
		cluster.setBrokerList("127.0.0.1:10911");
		cluster.setNameSvrAddr("127.0.0.1:9876");
		config.setCluster(cluster);
		producerService = new RocketMQ420ProducerService<>(config);
		producerService.start();
	}
	@AfterAll
	public static void destroy() {
		if (producerService != null) {
			producerService.stop();
		}
	}
	
	@Test
	public void testSendMessage() {
		ProducerPartition partition = new ProducerPartition(taskCode, toClusterCode, toTopic, toPartition, toBrokerName);
		String partitionKey = PartitionUtils.generatorPartitionKey(toBrokerName, toPartition);
		List<RktMQMessage> messageList = new ArrayList<>();
		int messageCount = 5;
		int messageIndex = 60;
		for (int i = messageIndex; i < messageIndex + messageCount; i++) {
			String messageId = "msgId" + i;
			String messageKey = "msgKey" + i;
			byte[] messageBytes = ("this is message " + i).getBytes();
			RktMQMessage message = new RktMQMessage(toTopic, partitionKey, messageId, messageKey, messageBytes);
			messageList.add(message);
		}
		CheckMessageBatchRequest<RktMQMessage> checkRequest = new CheckMessageBatchRequest<>(partition, messageList);
		CheckMessageResponse checkResponse = producerService.checkMessage(checkRequest);
		assertTrue(checkResponse.isSuccess());
		SendMessageRequest<RktMQMessage> request = new SendMessageRequest<>(partition, messageList);
		SendMessageResponse<RktMQMessage> response = producerService.sendMessage(request);
		System.out.println(response);
		assertTrue(response.isSuccess());
		assertNotNull(response.getData());
		assertNotNull(response.getData().getSuccCount());
		assertEquals(response.getData().getSuccCount(), messageCount);
	}
}
