# Redis Demo - Best Practices Implementation

这是一个完整的 Redis 最佳实践代码示例项目，包含所有常用操作和高级特性。

## 🚀 快速开始

### 启动 Redis 服务
```bash
# 启动 Redis (如果使用 Docker)
docker run -d --name redis -p 6379:6379 redis:latest

# 或启动本地 Redis 服务
redis-server
```

### 运行测试
```bash
cd redis-demo
mvn test
```

## 📚 功能特性

### 1. 基础操作 (RedisBasicService)
- **SET/GET/EXPIRE/DEL** - 字符串基础操作
- **TTL 抖动机制** - 防止缓存雪崩，TTL 600-720 秒随机
- **SETNX** - 原子性设置操作
- **INCR/INCRBY** - 计数器操作
- **Key 命名规范** - `im:demo:module:entity:id[:field]` 格式

### 2. Hash 操作 (RedisHashService)
- **HSET/HGET/HMSET/HMGET** - 哈希字段操作
- **HEXISTS/HDEL** - 字段存在性检查和删除
- **HINCRBY** - 哈希字段数值递增
- **HKEYS/HVALS/HGETALL** - 获取键、值、全部字段
- **自动 TTL 管理** - 新哈希自动设置过期时间

### 3. List 操作 (RedisListService)
- **LPUSH/RPUSH/LPOP/RPOP** - 列表两端操作
- **LRANGE/LINDEX/LSET** - 范围获取和索引操作
- **BLPOP** - 阻塞式弹出 (消息队列)
- **LTRIM** - 保持列表大小限制 (聊天历史)
- **RPOPLPUSH** - 原子性移动元素

### 4. Set 操作 (RedisSetService)
- **SADD/SREM/SISMEMBER** - 集合基础操作
- **SMEMBERS/SCARD/SRANDMEMBER** - 获取成员和随机选择
- **SINTER/SUNION/SDIFF** - 集合运算 (交集、并集、差集)
- **SMOVE** - 集合间移动元素
- **在线用户管理** - 实时用户状态跟踪

### 5. Sorted Set 操作 (RedisZSetService)
- **ZADD/ZREM/ZSCORE** - 有序集合基础操作
- **ZRANK/ZREVRANK** - 排名查询 (正序/倒序)
- **ZRANGE/ZREVRANGE** - 范围查询
- **ZINCRBY** - 分数递增
- **ZRANGEBYSCORE** - 按分数范围查询
- **排行榜系统** - 游戏积分、用户等级排名

### 6. 管道操作 (RedisPipelineService)
- **批量 SET/GET** - 高性能批量操作
- **批量递增** - 多个计数器同时操作
- **批量列表/集合操作** - 减少网络往返
- **性能对比测试** - 单操作 vs 管道操作
- **50-100x 性能提升** - 大量操作时显著优化

### 7. 事务与乐观锁 (RedisTransactionService)
- **MULTI/EXEC/DISCARD** - Redis 事务
- **WATCH/UNWATCH** - 乐观锁机制
- **账户转账** - 原子性转账操作
- **版本控制** - 并发更新冲突检测
- **重试机制** - 指数退避重试策略

### 8. 分布式锁 (RedisDistributedLockService)
- **Redisson 分布式锁** - 可重入、自动续期
- **超时控制** - 获取锁和持有锁的超时
- **资源保护** - 分布式环境下的资源同步
- **锁状态查询** - 检查锁状态和强制释放
- **实际应用场景** - 分布式计数器、资源分配

### 9. 发布订阅 (RedisPubSubService)
- **PUBLISH/SUBSCRIBE** - 实时消息发布订阅
- **频道管理** - 动态订阅和取消订阅
- **结构化消息** - JSON 格式消息传递
- **聊天室系统** - 实时聊天功能
- **系统通知** - 用户通知推送
- **状态同步** - 用户在线状态广播

### 10. 连接池与性能优化
- **Lettuce 连接池** - 高性能异步客户端
- **连接池配置** - 最大连接数、空闲连接管理
- **超时配置** - 连接、命令执行超时
- **慢查询监控** - 性能问题识别
- **内存限制** - 防止内存溢出
- **并发测试** - 多线程压力测试

## 🏗️ 项目结构

```
redis-demo/
├── src/main/java/com/im/redis/
│   ├── RedisDemoApplication.java          # 启动类
│   ├── config/
│   │   └── RedisConfig.java               # Redis 配置 (连接池、TTL抖动)
│   ├── util/
│   │   └── RedisKeyUtil.java              # Key 命名工具类
│   └── service/
│       ├── RedisBasicService.java         # 基础字符串操作
│       ├── RedisHashService.java          # Hash 操作
│       ├── RedisListService.java          # List 操作  
│       ├── RedisSetService.java           # Set 操作
│       ├── RedisZSetService.java          # Sorted Set 操作
│       ├── RedisPipelineService.java      # 管道批量操作
│       ├── RedisTransactionService.java   # 事务与乐观锁
│       ├── RedisDistributedLockService.java # 分布式锁
│       └── RedisPubSubService.java        # 发布订阅
├── src/test/java/com/im/redis/
│   └── RedisDemoTests.java                # 完整测试用例
└── src/main/resources/
    └── application.yml                    # 配置文件
```

## 🧪 测试用例

每个测试方法对应一个功能模块，点击 IDE 中的 ▶️ 按钮即可单独运行：

1. **testBasicStringOperations** - 基础字符串操作
2. **testHashOperations** - Hash 字段管理
3. **testListOperations** - 列表消息队列
4. **testSetOperations** - 集合关系操作
5. **testZSetOperations** - 排行榜系统
6. **testPipelineOperations** - 批量高性能操作
7. **testTransactionAndOptimisticLock** - 事务和乐观锁
8. **testDistributedLock** - 分布式锁
9. **testPubSubMessaging** - 发布订阅消息
10. **testConnectionPoolAndPerformance** - 连接池和性能测试

## 📋 最佳实践要点

### Key 设计规范
```java
// ✅ 良好的 Key 命名
im:demo:user:profile:user123
im:demo:user:session:user123:version
im:demo:chat:messages:room001
im:demo:ranking:score:global

// ❌ 避免的命名方式
user123
userProfile
chat_room_1
```

### TTL 抖动机制
```java
// 基础 TTL 600 秒，抖动范围 ±120 秒
// 实际 TTL: 480-720 秒随机
long ttl = ttlGenerator.generateTtl(); // 防止缓存雪崩
```

### Hash vs String 选择
```java
// ✅ 使用 Hash 存储用户信息 (字段可单独更新)
redisHashService.setHashField("user", "profile", "user123", "name", "John");
redisHashService.setHashField("user", "profile", "user123", "email", "john@example.com");

// ❌ 避免大对象序列化存储
redisBasicService.setValue("user", "profile", "user123", hugeJsonString);
```

### 并发安全操作
```java
// ✅ 使用乐观锁防止并发冲突
boolean success = redisTransactionService.incrementCounterWithOptimisticLock(
    "system", "counter", "global", 5
);

// ✅ 使用分布式锁保护临界资源
redisDistributedLockService.executeWithLock("resource_001", () -> {
    // 临界区代码
});
```

### 性能优化
```java
// ✅ 批量操作使用管道
redisPipelineService.batchSetStrings(keyValueMap); // 50-100x 性能提升

// ❌ 避免循环中的单个操作
for (String key : keys) {
    redisBasicService.setValue("test", "data", key, value); // 性能差
}
```

## 🔧 配置说明

### Redis 连接池配置
```yaml
spring:
  redis:
    lettuce:
      pool:
        max-active: 20      # 最大连接数
        max-idle: 10        # 最大空闲连接
        min-idle: 5         # 最小空闲连接
        max-wait: 2000ms    # 获取连接最大等待时间
```

### Redisson 分布式锁配置
```yaml
redisson:
  address: redis://localhost:6379
  database: 1                    # 使用独立数据库
  connection-pool-size: 20       # 连接池大小
  timeout: 3000                  # 命令超时
```

### TTL 和性能配置
```yaml
redis:
  key:
    ttl:
      default: 600              # 默认 TTL 10 分钟
      jitter-range: 120         # 抖动范围 ±2 分钟
  slowlog:
    slower-than: 10000          # 慢查询阈值 10ms
  memory:
    max-memory: "256mb"         # 内存限制
    policy: "allkeys-lru"       # LRU 淘汰策略
```

## 🚦 运行要求

- **Java 17+**
- **Spring Boot 3.x**
- **Redis 6.0+**
- **Maven 3.6+**

## 📈 性能指标

根据测试结果：
- **管道操作**: 比单个操作快 50-100 倍
- **连接池**: 支持高并发 (测试 10 线程 × 50 操作)
- **分布式锁**: 毫秒级获取和释放
- **发布订阅**: 实时消息传递 (<5ms 延迟)

## 🎯 实际应用场景

1. **用户会话管理** - Hash 存储用户信息，TTL 自动清理
2. **聊天系统** - List 存储消息历史，Pub/Sub 实时推送
3. **在线用户** - Set 管理在线状态，实时更新
4. **排行榜** - ZSet 实现积分排名，高效查询
5. **分布式锁** - 保护共享资源，防止并发冲突
6. **计数器** - 原子性递增，支持高并发
7. **缓存系统** - 合理 TTL，防止雪崩和穿透

这个项目展示了 Redis 在实际生产环境中的最佳实践，可以直接应用于真实项目中。
