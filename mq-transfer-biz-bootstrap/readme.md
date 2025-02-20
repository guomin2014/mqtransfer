IDEA启动说明：
1、VM参数中添加-Dsofa.ark.embed.enable=true -Dcom.alipay.sofa.ark.master.biz=mq-transfer-manager
2、需要先关闭provider相关的plugin(mq-provider-facade、mq-provider-kafka-082等)，否则不能加载plugin
3、单机模式：由管理端manager直接调用worker的类方法添加转发任务，集群模式：管理端manager将任务写入中间件(zookeeper)，worker通过监听zk变更来实现任务的分配与启停（一期通过helix操作中间件，后期改为直接操作中间件）
4、静态合并部署模式，manager与worker项目需要在关闭状态，且VM参数中添加-Dsofa.ark.embed.static.biz.enable=true
5、封装ark的上层框架：https://koupleless.io
6、java -Dsofa.ark.embed.enable=true -Dsofa.ark.embed.static.biz.enable=true -Dcom.alipay.sofa.ark.master.biz=mq-transfer-biz-bootstrap -jar mq-transfer-biz-bootstrap-1.0.0-SNAPSHOT-ark-biz.jar