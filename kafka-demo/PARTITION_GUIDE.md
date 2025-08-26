# Kafka 分区策略说明

## 分区确认和控制方法

### 1. 默认分区策略（轮询）
```java
kafkaTemplate.send(topic, message)
```
- **行为**: 使用轮询方式将消息分配到不同分区
- **适用场景**: 希望均匀分布消息负载
- **优点**: 分区负载均衡
- **缺点**: 无法保证相关消息在同一分区

### 2. 基于Key的分区
```java
kafkaTemplate.send(topic, key, message)
```
- **行为**: 根据key的hash值确定分区（相同key总是到同一分区）
- **适用场景**: 需要保证相关消息的顺序性
- **优点**: 相同key的消息有序，可预测分区
- **缺点**: 可能导致分区不均衡

### 3. 指定分区
```java
ProducerRecord<String, ChatMessage> record = new ProducerRecord<>(topic, partition, key, message);
kafkaTemplate.send(record);
```
- **行为**: 直接指定消息发送到哪个分区
- **适用场景**: 完全控制消息分布
- **优点**: 精确控制
- **缺点**: 需要手动管理分区负载

### 4. 业务逻辑分区
```java
// 基于业务规则生成key
String partitionKey = generatePartitionKey(sender, receiver);
kafkaTemplate.send(topic, partitionKey, message);
```
- **行为**: 根据业务逻辑（如对话双方）生成key
- **适用场景**: 聊天应用中确保同一对话的消息有序
- **优点**: 业务相关性强，消息有序
- **缺点**: 需要设计好的key生成策略

## 分区信息获取

### 从SendResult获取分区信息
```java
future.whenComplete((result, ex) -> {
    if (ex == null) {
        RecordMetadata metadata = result.getRecordMetadata();
        int partition = metadata.partition();
        long offset = metadata.offset();
        String topic = metadata.topic();
    }
});
```

### 获取Topic分区数量
```java
int partitionCount = kafkaTemplate.partitionsFor(topic).size();
```

## 测试方法

### 使用REST API测试
```bash
# 测试默认分区
curl -X POST "http://localhost:8080/api/test/partition/default?content=test"

# 测试指定key分区
curl -X POST "http://localhost:8080/api/test/partition/with-key?key=user1&content=test"

# 测试指定分区
curl -X POST "http://localhost:8080/api/test/partition/specific-partition?partition=0&content=test"

# 测试业务逻辑分区
curl -X POST "http://localhost:8080/api/test/partition/business-partition?sender=alice&receiver=bob&content=hello"

# 获取分区数量
curl -X GET "http://localhost:8080/api/test/partition/partition-count"

# 批量测试
curl -X POST "http://localhost:8080/api/test/partition/batch-test?messageCount=20"
```

### 使用ManualKafkaTest测试
运行现有的ManualKafkaTest工具也可以观察分区分布。

## 最佳实践

### 对于聊天应用
1. **使用业务逻辑分区**: 基于对话双方生成key，确保同一对话的消息有序
2. **监控分区分布**: 定期检查各分区的消息分布是否均匀
3. **合理设置分区数**: 根据并发用户数和消费者数量设置合适的分区数

### 分区Key设计原则
1. **唯一性**: key应该能唯一标识业务实体
2. **分布性**: key的hash值应该相对均匀分布
3. **业务意义**: key应该有明确的业务含义

## 注意事项

1. **分区数不能动态减少**: 只能增加分区，不能减少
2. **消费者数量**: 消费者数量不应超过分区数
3. **顺序保证**: 只有在同一分区内才能保证消息顺序
4. **负载均衡**: 需要平衡消息有序性和负载分布
