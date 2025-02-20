package com.gm.mqtransfer.provider.kafka.v230.service.consumer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;


public class MessageId {

    private static final int LEN_LIMIT = 1024;
    
    public static final String HEADER_SPLIT = "@";
    public static final String HEADER_EQUAL = "=";
    public static final String HEADER_SUB_SPLIT = ",";
    public static final String GRAY_SIGN_PREFIX = "___";

    private String realMessageId;
    private String packMessageId;
    private Map<String, String> props;

    private MessageId(String realMessageId, Map<String, String> props) {
        this.realMessageId = realMessageId;
        this.props = props;
        this.packMessageId = pack();
    }

    private MessageId(String packMessageId, String realMessageId, Map<String, String> props) {
        this.realMessageId = realMessageId;
        this.props = props;
        this.packMessageId = packMessageId;
    }

    private String pack() {
        if (this.realMessageId == null || "".equals(this.realMessageId.trim())) {
            this.realMessageId = uuid();
        }
        if (props == null || props.size() == 0) {
            return this.realMessageId;
        }

        StringBuilder builder = new StringBuilder(HEADER_SPLIT);
        for (Iterator<Map.Entry<String, String>> ite = props.entrySet().iterator(); ite.hasNext();) {
            Map.Entry<String, String> entry = ite.next();
            String key = entry.getKey(), value = entry.getValue();
            builder.append(key).append(HEADER_EQUAL).append(value).append(HEADER_SUB_SPLIT);
        }
        builder.deleteCharAt(builder.length() - 1);
        builder.append(HEADER_SPLIT);
        builder.append(this.realMessageId);

//        Preconditions.checkArgument(builder.length() <= LEN_LIMIT,
//                "pack message id length must <= " + LEN_LIMIT);

        return builder.toString();

    }

    private String uuid() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    public String getRealMessageId() {
        return realMessageId;
    }

    public String getPackMessageId() {
        return packMessageId;
    }

    public Map<String, String> getProps() {
        return props;
    }

    public static Builder create() {
        return new Builder();
    }

    public static MessageId parse(String packMessageId) {
        if (packMessageId == null || "".equals(packMessageId.trim())) {
            return null;
        }

        if (packMessageId.startsWith(HEADER_SPLIT)) {
            try {
                String[] msgArr = packMessageId.trim()
                        .substring(1, packMessageId.length())
                        .split(HEADER_SPLIT, -1);
                String[] propArr = msgArr[0].split("\\s*,\\s*");
                String realMessageId = msgArr[1];
                Map<String, String> props = null;
                if (propArr.length > 0) {
                    props = new HashMap<>();
                    for (String prop : propArr) {
                        String[] kv = prop.split(HEADER_EQUAL);
                        props.put(kv[0], kv[1]);
                    }
                }
                return new MessageId(packMessageId, realMessageId, props);
            } catch (Exception e) {
                return new MessageId(packMessageId, packMessageId, new HashMap<String, String>());
            }
        } else {
            return new MessageId(packMessageId, packMessageId, new HashMap<String, String>());
        }
    }

    public static class Builder {
        private String realMessageId;
        private Map<String, String> props;

        public Builder realMessageId(String realMessageId) {
            this.realMessageId = realMessageId;
            return this;
        }

        public Builder props(Map<String, String> props) {
            this.props = props;
            return this;
        }

        public Builder prop(String key, String val) {
            if (key == null) {
                return this;
            }
            if (this.props == null) {
                props = new HashMap<>();
            }
            this.props.put(key, val);
            return this;
        }

        public MessageId build() {
            return new MessageId(this.realMessageId, this.props);
        }

    }
}
