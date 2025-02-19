package com.gm.mqtransfer.provider.kafka.v082.service.consumer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gm.mqtransfer.provider.kafka.v082.common.Constants;
import com.google.common.collect.Lists;

import kafka.api.PartitionOffsetRequestInfo;
import kafka.common.ErrorMapping;
import kafka.common.TopicAndPartition;
import kafka.consumer.ConsumerConfig;
import kafka.javaapi.OffsetRequest;
import kafka.javaapi.OffsetResponse;
import kafka.javaapi.PartitionMetadata;
import kafka.javaapi.TopicMetadata;
import kafka.javaapi.TopicMetadataRequest;
import kafka.javaapi.TopicMetadataResponse;
import kafka.javaapi.consumer.SimpleConsumer;


/**
 * kafka cluster manager;
 */
public class Kafka08Cluster {
    private static final Logger LOG = LoggerFactory.getLogger(Kafka08Cluster.class);

    public ConsumerConfig config;
    
    List<KafkaBroker> seedBrokers;

    /**
     * TopicPartition and SimpleConsumer mapping;
     */
    public Map<KafkaBroker, SimpleConsumer> brokerConusmerMap = new ConcurrentHashMap<>();
    
    public Kafka08Cluster(ConsumerConfig config){
        this.config = config;
        String brokers = config.props().getString(Constants.KafkaConsumer.BOOTSTRAP_SERVERS_NAME);
        seedBrokers = new ArrayList<>();
        String[] brokerArrs = brokers.split(",");
        for (String broker : brokerArrs) {
        	String[] hp = broker.split(":");
            if(hp.length==1)
                throw new IllegalArgumentException("Broker not in the correct format of <host>:<port> ["+broker+"]");
            seedBrokers.add(new KafkaBroker(hp[0],Integer.parseInt(hp[1])));
        }
    }
    private SimpleConsumer getOrCreateConsumer(String host, int port, boolean share) {
        KafkaBroker broker = new KafkaBroker(host,port);
        if (share) {
        	SimpleConsumer consumer = brokerConusmerMap.get(broker);
            synchronized(this) {
                if(consumer==null){
                    consumer = new SimpleConsumer(host, port, config.socketTimeoutMs(),
                            config.socketReceiveBufferBytes(), config.clientId());
                    brokerConusmerMap.put(broker,consumer);
                }
            }
            return consumer;
        } else {
        	return new SimpleConsumer(host, port, config.socketTimeoutMs(),
                    config.socketReceiveBufferBytes(), config.clientId());
        }
    }

    public SimpleConsumer connectLeader(String topic, int partition, boolean share){
        KafkaBroker leader = findLeader(topic, partition);
        return getOrCreateConsumer(leader.host(), leader.port(), share);
    }

    /**
     * 从 kafka 中找出指定分区的leader;
     * @param topic
     * @param partition
     * @return
     */
    public KafkaBroker findLeader(String topic,int partition){
        TopicMetadataRequest req = new TopicMetadataRequest(
                kafka.api.TopicMetadataRequest.CurrentVersion(),
                0,
                config.clientId(),
                Lists.newArrayList(topic));
        List<KafkaBroker> brokers = Lists.newArrayList(this.seedBrokers);
        Collections.shuffle(brokers);

        for(KafkaBroker b: brokers){
            SimpleConsumer c = null;
            try{
                c = getOrCreateConsumer(b.host(),b.port(), true);
                TopicMetadataResponse res = c.send(req);
                List<TopicMetadata> metaList = res.topicsMetadata();
                for (TopicMetadata meta : metaList) {
                	if (meta.topic().equals(topic)) {
                		List<PartitionMetadata> partitionMetaList = meta.partitionsMetadata();
                		for (PartitionMetadata pmeta : partitionMetaList) {
                			if (pmeta.partitionId() == partition) {
                				return new KafkaBroker(pmeta.leader().host(),pmeta.leader().port());
                			}
                		}
                	}
                }
            }catch (Exception e){
                LOG.error("counter error:",e);
            }
        }
        throw new RuntimeException("cannot find leader for[topic={"+topic+"},partition={"+partition+"}]");
    }

    private Map<TopicAndPartition,KafkaBroker> findLeaders(List<TopicAndPartition> topicAndPartitions){
        Map<TopicAndPartition, KafkaBroker> result = new HashMap<>(topicAndPartitions.size());
        List<String> topics = new ArrayList<>();
        Map<String, TopicAndPartition> tpMap = new HashMap<>();
        for (TopicAndPartition tp : topicAndPartitions) {
        	topics.add(tp.topic());
        	String key = tp.topic() + "###" + tp.partition();
        	tpMap.put(key, tp);
        }
        List<TopicMetadata> topicMetaList = getPartitionMetadatas(topics);
        for (TopicMetadata tm : topicMetaList) {
        	String topic = tm.topic();
        	List<PartitionMetadata> pmList = tm.partitionsMetadata();
        	for (PartitionMetadata pm : pmList) {
        		String key = topic + "###" + pm.partitionId();
        		if (tpMap.containsKey(key)) {//这里我们只需要入参中的分区的leader;
        			result.put(tpMap.get(key), new KafkaBroker(pm.leader().host(),pm.leader().port()));
        		}
        	}
        }
        return result;
    }

    public List<TopicAndPartition> getPartitions(List<String> topics){
    	List<TopicAndPartition> retList = new ArrayList<>();
    	List<TopicMetadata> topicMetaList = getPartitionMetadatas(topics);
        for (TopicMetadata tm : topicMetaList) {
//        	String topic = tm.topic();
        	List<PartitionMetadata> pmList = tm.partitionsMetadata();
        	for (PartitionMetadata pm : pmList) {
        		retList.add(new TopicAndPartition(tm.topic(),pm.partitionId()));
        	}
        }
        return retList;
    }

    public List<TopicMetadata> getPartitionMetadatas(List<String> topics){
        TopicMetadataRequest req = new TopicMetadataRequest(
                kafka.api.TopicMetadataRequest.CurrentVersion(),
                0,
                config.clientId(),
                topics);
        List<KafkaBroker> brokers = Lists.newArrayList(this.seedBrokers);
        Collections.shuffle(brokers);
        for(KafkaBroker b: brokers){
            SimpleConsumer c = null;
            try{
                c = getOrCreateConsumer(b.host(),b.port(), true);
                TopicMetadataResponse res = c.send(req);
                List<TopicMetadata> tmList = res.topicsMetadata();
                boolean hasError = false;
                for (TopicMetadata tm : tmList) {
                	if (tm.errorCode() != ErrorMapping.NoError()) {
                		hasError = true;
                		break;
                	}
                }
                if (!hasError) {
                	return tmList;
                }
            }catch (Exception e){
                LOG.error("getPartitionMetadatas error:",e);
            }
        }
        throw new RuntimeException("cannot find partitions for[topic={"+ topics +"}]");
    }

    public Map<TopicAndPartition, LeaderOffset> getLatestLeaderOffset(List<TopicAndPartition> topicAndPartitions){
        //// -1L表示：OffsetRequest.LatestTime,这个常量在javaapi中没有定义;
        return getLeaderOffsets(topicAndPartitions,-1L);
    }

    public Map<TopicAndPartition,LeaderOffset> getEarliestLeaderOffset(List<TopicAndPartition> topicAndPartitions){
        // -2L表示：OffsetRequest.EarliestTime,这个常量在javaapi中没有定义;
        return getLeaderOffsets(topicAndPartitions,-2L);
    }

    public Map<TopicAndPartition,LeaderOffset> getLeaderOffsets(List<TopicAndPartition> topicAndPartitions,long before){
        Map<TopicAndPartition,LeaderOffset> result = new HashMap<>();
        Map<TopicAndPartition, List<LeaderOffset>> map = getLeaderOffsets(topicAndPartitions,before,1);
        for (Map.Entry<TopicAndPartition, List<LeaderOffset>> entry : map.entrySet()) {
        	List<LeaderOffset> list = entry.getValue();
        	if (list.size() > 0) {
        		result.put(entry.getKey(), list.get(0));
        	}
        }
        
        return result;
    }

    public Map<TopicAndPartition,List<LeaderOffset>> getLeaderOffsets(List<TopicAndPartition> topicAndPartitions,
                                                        long before,int maxNumOffsets){
        Map<TopicAndPartition, List<LeaderOffset>> result = new HashMap<>();
        Map<KafkaBroker, List<TopicAndPartition>> leaderToTp = new HashMap<>();
        Map<TopicAndPartition, KafkaBroker> tpMap = this.findLeaders(topicAndPartitions);
        for (Map.Entry<TopicAndPartition, KafkaBroker> entry : tpMap.entrySet()) {
        	TopicAndPartition tp = entry.getKey();
        	KafkaBroker broker = entry.getValue();
        	if (leaderToTp.containsKey(broker)) {
        		leaderToTp.get(broker).add(tp);
        	} else {
        		List<TopicAndPartition> list = new ArrayList<>();
        		list.add(tp);
        		leaderToTp.put(broker, list);
        	}
        }
        for (Map.Entry<KafkaBroker, List<TopicAndPartition>> entry : leaderToTp.entrySet()) {
        	KafkaBroker leader = entry.getKey();
        	List<TopicAndPartition> tps = entry.getValue();
        	SimpleConsumer c = null;
            try{
                c = getOrCreateConsumer(leader.host(),leader.port(), true);
                Map<TopicAndPartition, PartitionOffsetRequestInfo> reqMap = new HashMap<>(tps.size());
                for (TopicAndPartition tp : tps) {
                	reqMap.put(tp, new PartitionOffsetRequestInfo(before,maxNumOffsets));
                }
                OffsetRequest req = new OffsetRequest(reqMap, kafka.api.OffsetRequest.CurrentVersion(),"");
                OffsetResponse resp = c.getOffsetsBefore(req);
                if(!resp.hasError()){
                	for (TopicAndPartition tp : tps) {
                		List<LeaderOffset> leaderOffsets = new ArrayList<>();
                		long[] offsets = resp.offsets(tp.topic(), tp.partition());
                		for (long offset : offsets) {
                			leaderOffsets.add(new LeaderOffset(leader.host(),leader.port(),offset));
                		}
                		result.put(tp, leaderOffsets);
                    }
                } else {
                	for (TopicAndPartition tp : tps) {
                		Throwable e = ErrorMapping.exceptionFor(resp.errorCode(tp.topic(),tp.partition()));
                		if (e != null) {
                			throw new RuntimeException(e);
                		}
                	}
                }
            }catch (Exception e){
                LOG.error("encounter error:",e);
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    public List<OffsetRange> createOffsetRange(Map<TopicAndPartition,Long> fromOffsets, Integer partitionBatchSize) {
        Map<TopicAndPartition, LeaderOffset> leaderOffsets = getLatestLeaderOffset(Lists.newArrayList(fromOffsets.keySet()));
        List<OffsetRange> retList = new ArrayList<>();
        for (Map.Entry<TopicAndPartition, LeaderOffset> entry : leaderOffsets.entrySet()) {
        	TopicAndPartition tp = entry.getKey();
        	LeaderOffset leaderOffset = entry.getValue();
        	long untilOffset = Math.min(fromOffsets.get(tp) + partitionBatchSize, leaderOffset.offset);
        	retList.add(new OffsetRange(tp.topic(), tp.partition(), fromOffsets.get(tp), untilOffset));
        }
        return retList;
    }
    
    public void close() {
    	for (Map.Entry<KafkaBroker, SimpleConsumer> entry : brokerConusmerMap.entrySet()) {
			try {
				entry.getValue().close();
			} catch (Exception e) {}
		}
    	brokerConusmerMap.clear();
    }
    
    public void close(SimpleConsumer consumer) {
    	KafkaBroker broker = new KafkaBroker(consumer.host(), consumer.port());
    	if (brokerConusmerMap.containsKey(broker)) {
    		synchronized(this) {
    			SimpleConsumer oldConsumer = brokerConusmerMap.get(broker);
    			if (oldConsumer != null && oldConsumer == consumer) {
    				brokerConusmerMap.remove(broker);
    			}
    			consumer.close();
    		}
    	}
    }

}
