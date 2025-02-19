package com.gm.mqtransfer.provider.kafka.v230.service.consumer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.gm.mqtransfer.provider.facade.api.FetchOffsetRangeRequest;
import com.gm.mqtransfer.provider.facade.api.FetchOffsetRangeResponse;
import com.gm.mqtransfer.provider.facade.api.PollMessageRequest;
import com.gm.mqtransfer.provider.facade.api.PollMessageResponse;
import com.gm.mqtransfer.provider.facade.api.SubscribeRequest;
import com.gm.mqtransfer.provider.facade.api.SubscribeResponse;
import com.gm.mqtransfer.provider.facade.model.ConsumerClientConfig;
import com.gm.mqtransfer.provider.facade.model.ConsumerPartition;
import com.gm.mqtransfer.provider.facade.model.KafkaClusterInfo;

public class Kafka230ConsumerServiceTest {

	private static Kafka230ConsumerService consumerService;
	
	static ConsumerPartition consumerPartition;
	
	@BeforeAll
	public static void init() {
		String taskCode = "task1";
		String fromClusterCode = "bigdata";
		ConsumerClientConfig config = new ConsumerClientConfig();
		config.setClientShare(true);
		config.setInstanceIndex(1);
		config.setInstanceCode(taskCode);
		config.setConsumerProps(new Properties());
		KafkaClusterInfo cluster = new KafkaClusterInfo();
		cluster.setCode(fromClusterCode);
		cluster.setBrokerList("127.0.0.1:9092");
		cluster.setZkUrl("127.0.0.1:2181");
		config.setCluster(cluster);
		consumerService = new Kafka230ConsumerService(config);
		consumerService.start();
		String fromTopic = "kb-source-01";
		int fromPartition = 1;
		String fromBrokerName = null;
		String expectStartOffset = null;
		Long expectStartOffsetValue = null;
		consumerPartition = new ConsumerPartition(taskCode, fromClusterCode, fromTopic, fromPartition, fromBrokerName, expectStartOffset, expectStartOffsetValue);
	}
	@AfterAll
	public static void destroy() {
		if (consumerService != null) {
			consumerService.stop();
		}
	}
	@Test
	public void testSubscribe() {
		SubscribeRequest request = new SubscribeRequest(consumerPartition);
		SubscribeResponse response = consumerService.subscribe(request);
		assertTrue(response.isSuccess());
		assertNotNull(response.getData().getStartOffset());
		assertNotNull(response.getData().getEndOffset());
	}
	@Test
	public void testFetchOffsetRange() {
		FetchOffsetRangeRequest fetchOffsetRangeRequest = new FetchOffsetRangeRequest(consumerPartition);
		FetchOffsetRangeResponse response = consumerService.fetchOffsetRange(fetchOffsetRangeRequest);
		assertTrue(response.isSuccess());
		assertNotNull(response.getData().getStartOffset());
		assertNotNull(response.getData().getEndOffset());
	}
	@Test
	public void testPollMessage() {
//		consumerPartition.setStartOffset(0L);
		SubscribeRequest subscribeRequest = new SubscribeRequest(consumerPartition);
		SubscribeResponse subscribeResponse = consumerService.subscribe(subscribeRequest);
		assertTrue(subscribeResponse.isSuccess());
		PollMessageRequest request = new PollMessageRequest(consumerPartition, 10, 102400L);
		PollMessageResponse response = consumerService.pollMessage(request);
		assertTrue(response.isSuccess());
		assertNotNull(response.getData().getMessages());
		assertNotNull(response.getData().getEndOffset());
		assertNotNull(response.getData().getHighWatermark());
	}
}
