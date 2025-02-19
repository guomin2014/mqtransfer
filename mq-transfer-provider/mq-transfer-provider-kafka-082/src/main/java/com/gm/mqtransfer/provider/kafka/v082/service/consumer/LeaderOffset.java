package com.gm.mqtransfer.provider.kafka.v082.service.consumer;

public class LeaderOffset {
    public String host;
    public int port;
    public long offset;

    public LeaderOffset(String host,int port,long offset){
        this.host = host;
        this.port = port;
        this.offset = offset;
    }
}
