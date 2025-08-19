# 腾讯云IM后端服务器项目

## 项目概述

这是一个基于腾讯云IM的企业级即时通讯后端服务器项目，实现了长连接维护、心跳检测、消息有序性保障等核心技术，并集成了时序数据库（InfluxDB/TDengine）用于历史消息存储。

## 技术架构

### 核心技术栈
- **Java 17** + **Spring Boot 3.2** + **Spring Cloud 2023**
- **长连接管理**: WebSocket + Netty
- **心跳检测**: 基于Redis的分布式心跳管理
- **消息有序性**: Redis原子操作保障消息序列号
- **时序数据库**: InfluxDB/TDengine历史消息存储
- **微服务治理**: Nacos注册中心与配置中心
- **缓存**: Redis分布式缓存
- **消息队列**: Kafka异步消息处理
- **监控**: Prometheus + Grafana + Jaeger

### 服务架构
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  auth-service   │    │connection-service│   │sequence-service │
│   (认证服务)      │    │   (连接服务)      │    │  (序列号服务)     │
│   Port: 8081    │    │   Port: 8082     │    │   Port: 8083    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                ┌─────────────────┴─────────────────┐
                │         基础设施层                 │
                │  Nacos + Redis + Kafka            │
                │  InfluxDB + MySQL + Prometheus    │
                └───────────────────────────────────┘
```

## 核心功能

### 1. 长连接维护 (Connection Service)
- **WebSocket连接管理**: 支持多设备同时在线
- **连接池管理**: 基于Redis的分布式连接池
- **故障转移**: 连接断开自动重连机制
- **负载均衡**: 支持多实例部署的连接分配

### 2. 心跳检测 (Heartbeat Detection)
- **定时心跳**: 30秒间隔的心跳检测
- **超时处理**: 60秒超时自动断开连接
- **状态同步**: Redis实时同步用户在线状态
- **异常恢复**: 网络异常后的状态恢复机制

### 3. 消息有序性保障 (Message Sequence)
- **原子序列号**: Redis原子操作生成全局唯一序列号
- **会话隔离**: 单聊/群聊序列号独立管理
- **顺序保证**: 严格按序列号顺序处理消息
- **幂等处理**: 重复消息自动去重

### 4. 历史消息存储 (TimeSeries Database)
- **InfluxDB**: 高性能时序数据存储
- **TDengine**: 可选的国产时序数据库
- **数据分片**: 按时间和用户维度分片存储
- **查询优化**: 支持时间范围和条件查询

## 快速开始

### 1. 环境要求
- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- 内存: 8GB+
- 磁盘: 10GB+

### 2. 启动基础设施
```bash
# 启动基础设施服务
./start-infrastructure.sh

# 或者手动启动
docker-compose up -d
```

### 3. 启动应用服务

**启动认证服务:**
```bash
cd services/auth-service
mvn spring-boot:run
```

**启动连接服务:**
```bash
cd services/connection-service
mvn spring-boot:run
```

**启动序列号服务:**
```bash
cd services/message-sequence-service
mvn spring-boot:run
```

### 4. 验证服务
```bash
# 检查认证服务
curl http://localhost:8081/api/auth/verify

# 检查连接服务
curl http://localhost:8082/api/connection/stats

# 检查序列号服务
curl http://localhost:8083/api/sequence/c2c/current?fromUserId=user001&toUserId=user002
```

## API文档

服务启动后，可通过以下地址查看API文档：
- 认证服务: http://localhost:8081/doc.html
- 连接服务: http://localhost:8082/doc.html
- 序列号服务: http://localhost:8083/doc.html

## 配置说明

### 腾讯云IM配置
在 `application.yml` 中配置：
```yaml
tencent:
  im:
    sdk-app-id: 1400000000  # 你的SDKAppID
    secret-key: your_secret_key  # 你的密钥
```

### 时序数据库配置
```yaml
timeseries:
  type: INFLUXDB  # 或 TDENGINE
  influxdb:
    url: http://localhost:8086
    token: your_token
    org: im-org
    bucket: im-messages
```

## 监控与运维

### 服务监控
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin123456)
- **Jaeger**: http://localhost:16686

### 关键指标
- 连接数统计
- 心跳成功率
- 消息序列号生成速率
- 时序数据库写入性能

## 测试示例

### 1. WebSocket连接测试
```javascript
// 前端连接示例
const ws = new WebSocket('ws://localhost:8082/ws/im?userId=user001');

ws.onopen = function() {
    console.log('Connected');
    // 发送心跳
    setInterval(() => {
        ws.send(JSON.stringify({type: 'heartbeat'}));
    }, 30000);
};

ws.onmessage = function(event) {
    const message = JSON.parse(event.data);
    console.log('Received:', message);
};
```

### 2. 认证流程测试
```bash
# 1. 用户登录获取Token和UserSig
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser1",
    "password": "123456"
  }'

# 2. 使用Token获取新的UserSig
curl -X GET http://localhost:8081/api/auth/usersig/user001 \
  -H "Authorization: Bearer your_jwt_token"
```

## 项目结构

```
IM-backend/
├── common/                     # 公共模块
│   ├── common-core/           # 核心公共组件
│   ├── common-websocket/      # WebSocket公共组件
│   └── common-timeseries/     # 时序数据库公共组件
├── services/                   # 微服务
│   ├── auth-service/          # 认证服务
│   ├── connection-service/    # 连接服务
│   └── message-sequence-service/ # 序列号服务
├── docker/                     # Docker配置
└── docker-compose.yml         # 基础设施编排
```

## 扩展计划

- [ ] 消息回调服务 (callbacks-service)
- [ ] 消息编排服务 (chat-orchestration-service)
- [ ] 内容审核服务 (moderation-service)
- [ ] API网关 (gateway)
- [ ] 用户服务 (user-service)

## 联系方式

如有问题，请提交Issue或联系项目维护者。

## 许可证

本项目采用 MIT 许可证。
