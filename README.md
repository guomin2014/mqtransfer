## MQTransfer

[![License][license-image]][license-url]
[![Release][release-image]][release-url]

MQTransfer is a distributed message forwarding platform with low latency, high performance and reliability, trillion-level capacity and flexible scalability.

It offers a variety of features:

* From Kafka consumption messages to RocketMQ
* From RocketMQ consumption messages to Kafka
* Interacting messages from other message middleware [future]
* Message retroactivity by offset

----------


## Quick Start

This paragraph guides you through steps of installing MQTransfer in different ways.
For local development and testing, only one instance will be created for each component.

### Run MQTransfer locally

MQTransfer runs on all major operating systems and requires only a Java JDK version 8 or higher to be installed.
To check, run `java -version`:
```shell
$ java -version
java version "1.8.0_121"
```

MQTransfer utilizes Maven as a distribution management and packaging tool. Version 3.8.1 or later is required.
Maven installation and configuration instructions can be found here:

http://maven.apache.org/run-maven/index.html
    
Then, To check if Zookeeper exists and has been started

The recommended version for Zookeeper is 3.6.4


#### For macOS and Linux users, execute following commands:

**1) Config zookeeper url**

open file mq-biz/mq-transfer-biz-manager/src/main/resources/application-dev.yml 

and mq-biz/mq-transfer-biz-worker/src/main/resources/application-dev.yml

modify zookeeper url

```shell
transfer:
  cluster:
    zkUrl: 127.0.0.1:2181    ##zookeeper url
  module:
    storage:
      zkUrl: 127.0.0.1:2181  ##zookeeper url
```

**2) Build**
```shell
mvn clean install -Dmaven.test.skip=true -Prelease-all
```

**3) Start transfer bootstrap**
```shell
sh mq-transfer-biz-bootstrap/target/mq-transfer-biz-bootstrap/bin/start.sh
```

**4) visit web**

visit: http://127.0.0.1:8080/manager

username:admin,password:123456

first: create mq cluster;

then: create mq transfer task;


### IDEA Run MQTransfer

1. Add VM arguments: -Dsofa.ark.embed.enable=true -Dsofa.ark.embed.static.biz.enable=true -Dcom.alipay.sofa.ark.master.biz=mq-transfer-biz-bootstrap
2. If plugins are introduced in Eclipse, please close them first(mq-provider-facade、mq-provider-kafka-082、mq-provider-kafka-230)
3. If mq-transfer-biz-manager and mq-transfer-biz-worker are introduced in Eclipse, please close them first
4. maven build mq-transfer
5. find TransferBootstrapApplication in mq-transfer-biz-bootstrap, run it


----------
## License
[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html) Copyright (C) Apache Software Foundation


[license-image]: https://img.shields.io/badge/license-Apache%202-4EB1BA.svg
[license-url]: https://www.apache.org/licenses/LICENSE-2.0.html
[release-image]: https://img.shields.io/badge/release-download-orange.svg
[release-url]: https://www.apache.org/licenses/LICENSE-2.0.html
