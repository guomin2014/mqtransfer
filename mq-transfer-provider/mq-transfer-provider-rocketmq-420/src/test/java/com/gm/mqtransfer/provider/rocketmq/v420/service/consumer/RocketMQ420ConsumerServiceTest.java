package com.gm.mqtransfer.provider.rocketmq.v420.service.consumer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.gm.mqtransfer.provider.facade.api.CommitOffsetRequest;
import com.gm.mqtransfer.provider.facade.api.CommitOffsetResponse;
import com.gm.mqtransfer.provider.facade.api.PollMessageRequest;
import com.gm.mqtransfer.provider.facade.api.PollMessageResponse;
import com.gm.mqtransfer.provider.facade.api.SubscribeRequest;
import com.gm.mqtransfer.provider.facade.api.SubscribeResponse;
import com.gm.mqtransfer.provider.facade.model.ConsumerClientConfig;
import com.gm.mqtransfer.provider.facade.model.ConsumerPartition;
import com.gm.mqtransfer.provider.facade.model.RocketMQClusterInfo;

public class RocketMQ420ConsumerServiceTest {

	static RocketMQ420ConsumerService consumerService;
	static String taskCode = "task1";
	static String fromClusterCode = "bigdata";
	static String fromTopic = "rkt-source-01";
	static int fromPartition = 1;
	static String fromBrokerName = "broker-a";
	static String expectStartOffset = null;
	static Long expectStartOffsetValue = null;
	@BeforeAll
	public static void init() {
		ConsumerClientConfig config = new ConsumerClientConfig();
		config.setClientShare(true);
		config.setInstanceIndex(1);
		config.setInstanceCode(taskCode);
		config.setConsumerProps(new Properties());
		RocketMQClusterInfo cluster = new RocketMQClusterInfo();
		cluster.setCode(fromClusterCode);
		cluster.setBrokerList("127.0.0.1:10911");
		cluster.setNameSvrAddr("127.0.0.1:9876");
		config.setCluster(cluster);
		consumerService = new RocketMQ420ConsumerService(config);
		consumerService.start();
	}
	@AfterAll
	public static void destroy() {
		if (consumerService != null) {
			consumerService.stop();
		}
	}
	@Test
	public void testSubscribe() {
		ConsumerPartition consumerPartition = new ConsumerPartition(taskCode, fromClusterCode, fromTopic, fromPartition, fromBrokerName, expectStartOffset, expectStartOffsetValue);
		SubscribeRequest request = new SubscribeRequest(consumerPartition);
		SubscribeResponse response = consumerService.subscribe(request);
		System.out.println(response);
		assertTrue(response.isSuccess());
		assertNotNull(response.getData().getStartOffset());
		assertNotNull(response.getData().getEndOffset());
	}
	@Test
	public void testPollMessage() {
		ConsumerPartition consumerPartition = new ConsumerPartition(taskCode, fromClusterCode, fromTopic, fromPartition, fromBrokerName, expectStartOffset, expectStartOffsetValue);
		SubscribeRequest subscribeRequest = new SubscribeRequest(consumerPartition);
		SubscribeResponse subscribeResponse = consumerService.subscribe(subscribeRequest);
		System.out.println(subscribeResponse);
		assertTrue(subscribeResponse.isSuccess());
		assertNotNull(subscribeResponse.getData().getConsumerOffset());
		consumerPartition.setStartOffset(subscribeResponse.getData().getConsumerOffset());
		PollMessageRequest request = new PollMessageRequest(consumerPartition, 2, 102400L);
		PollMessageResponse response = consumerService.pollMessage(request);
		System.out.println(response);
		assertTrue(response.isSuccess());
		assertNotNull(response.getData().getEndOffset());
		CommitOffsetRequest commitOffsetRequest = new CommitOffsetRequest(consumerPartition, response.getData().getEndOffset() + 1);
		CommitOffsetResponse commitOffsetResponse = consumerService.commitOffset(commitOffsetRequest);
		System.out.println(commitOffsetResponse);
		assertTrue(commitOffsetResponse.isSuccess());
	}
}
