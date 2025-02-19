package com.gm.mqtransfer.provider.kafka.v082.service.consumer;

import kafka.common.TopicAndPartition;

public class OffsetRange {
    public String topic;
    public int partition;
    public long fromOffset;
    public long untilOffset;

    public OffsetRange(String topic,int partition,long fromOffset,long untilOffset){
        this.topic = topic;
        this.partition = partition;
        this.fromOffset = fromOffset;
        this.untilOffset = untilOffset;
    }

    public TopicAndPartition getTopicAndPartition(){
        return new TopicAndPartition(topic,partition);
    }

    public long getUntilOffset() {
        return untilOffset;
    }

    public int getPartition() {
        return partition;
    }

    public boolean isValid(){
        return fromOffset > -1 && untilOffset > -1 &&(untilOffset > fromOffset);
    }

    public long getDeltaMessage() {
        return untilOffset - fromOffset;
    }
}
