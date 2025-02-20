package com.gm.mqtransfer.provider.kafka.v230.service.consumer;

public class KafkaBroker {
    String host;
    int port;

    public KafkaBroker(String host,int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (!(obj instanceof KafkaBroker))
            return false;
        else {
            KafkaBroker b = (KafkaBroker)obj;
            if (this.host.equals(b.host) && this.port == b.port)
                return true;
            else
                return false;
        }

    }

    @Override
    public int hashCode() {
        return (host + port).hashCode();
    }

    public String host() {
        return host;
    }

    public int port() {
        return port;
    }
}
