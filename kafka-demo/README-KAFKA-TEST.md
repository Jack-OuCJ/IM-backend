# Kafka 生产消费能力测试

这个项目包含了多种Kafka生产消费能力测试，用于评估Kafka集群的性能表现。

## 前置条件

1. 确保Kafka集群正在运行
2. 确保配置文件中的Kafka连接信息正确

## 测试类说明

### 1. KafkaIntegrationTest
集成测试类，测试Kafka的基本生产消费功能。

**测试内容：**
- 单条消息生产消费测试
- 批量消息生产消费测试（10条）
- 高并发消息测试（100条）
- 大消息测试（不同大小的消息）

**运行方式：**
```bash
mvn test -Dtest=KafkaIntegrationTest
```

### 2. KafkaBenchmarkTest
基准测试类，专注于性能测试。

**测试内容：**
- 生产者吞吐量测试（1000条消息，10线程）
- 生产者延迟测试（100条消息）
- 不同消息大小性能测试
- 压力测试（30秒持续发送）

**运行方式：**
```bash
mvn test -Dtest=KafkaBenchmarkTest
```

### 3. KafkaPerformanceTest
性能测试类，使用嵌入式Kafka进行测试。

**测试内容：**
- 生产者性能测试
- 生产消费端到端性能测试
- 并发生产者测试
- 不同消息大小性能测试

**运行方式：**
```bash
mvn test -Dtest=KafkaPerformanceTest
```

### 4. KafkaTestRunner
交互式测试工具，可以手动选择不同的测试场景。

**功能：**
- 单条消息测试
- 批量消息测试（100条）
- 高吞吐量测试（1000条）
- 大消息测试
- 并发生产者测试
- 自定义测试

**运行方式：**
```bash
mvn spring-boot:run -Dspring-boot.run.main-class=com.im.KafkaTestRunner -Dspring.profiles.active=test
```

## 快速开始

### 1. 启动Kafka集群
```bash
# 在项目根目录下
docker compose -f docker-kafka.yml up -d
```

### 2. 验证Kafka状态
```bash
docker ps | grep kafka
```

### 3. 运行基本集成测试
```bash
cd kafka-demo
mvn test -Dtest=KafkaIntegrationTest
```

### 4. 运行性能基准测试
```bash
mvn test -Dtest=KafkaBenchmarkTest
```

### 5. 运行交互式测试工具
```bash
mvn spring-boot:run -Dspring-boot.run.main-class=com.im.KafkaTestRunner -Dspring.profiles.active=test
```

## 测试结果示例

### 吞吐量测试结果
```
基准测试完成
实际发送消息数: 1000
总耗时: 2341 ms
平均吞吐量: 427.25 消息/秒
平均延迟: 2.34 ms/消息
```

### 延迟测试结果
```
延迟基准测试完成
平均延迟: 1.23 ms
最小延迟: 0 ms
最大延迟: 15 ms
95百分位延迟: 3 ms
```

### 不同消息大小测试结果
```
消息大小: 100 字节
发送 50 条消息耗时: 234 ms
消息吞吐量: 213.68 消息/秒
数据吞吐量: 0.02 MB/秒

消息大小: 10000 字节
发送 50 条消息耗时: 567 ms
消息吞吐量: 88.18 消息/秒
数据吞吐量: 0.84 MB/秒
```

## 配置说明

### application.yml 关键配置
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092,localhost:9094,localhost:9096
    topic:
      name: chat-message-topic
      partitions: 3
      replication-factor: 2
    producer:
      acks: all
      retries: 3
      batch-size: 16384
    consumer:
      group-id: im-consumer-group
      auto-offset-reset: earliest
```

### 性能调优建议

1. **生产者优化：**
   - 增加 `batch.size` 提高批处理效率
   - 调整 `linger.ms` 控制延迟
   - 设置 `acks=all` 保证数据可靠性

2. **消费者优化：**
   - 使用手动提交 `enable-auto-commit: false`
   - 调整 `max.poll.records` 控制批量处理大小

3. **Topic优化：**
   - 增加分区数提高并行度
   - 设置合适的副本数保证可用性

## 故障排除

### 常见问题

1. **连接失败**
   ```
   检查Kafka集群是否启动：docker ps | grep kafka
   检查端口是否正确：9092, 9094, 9096
   ```

2. **超时错误**
   ```
   增加测试超时时间
   检查网络连接
   检查Kafka集群负载
   ```

3. **内存不足**
   ```
   调整JVM参数：-Xmx2g -Xms1g
   减少并发线程数
   减少批量大小
   ```

## 监控指标

测试过程中会输出以下关键指标：

- **吞吐量（Throughput）**: 每秒处理的消息数
- **延迟（Latency）**: 消息处理的平均时间
- **错误率（Error Rate）**: 失败消息的百分比
- **数据传输速率**: 每秒传输的数据量（MB/s）

## 扩展测试

你可以根据需要修改测试参数：

- 调整消息数量和大小
- 修改并发线程数
- 添加新的测试场景
- 集成监控工具（如Prometheus）

## 注意事项

1. 测试前确保Kafka集群资源充足
2. 大规模测试可能影响其他应用
3. 建议在测试环境中运行压力测试
4. 定期清理测试数据和日志
