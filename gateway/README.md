# Gateway 微服务

## 概述

Gateway微服务是IM后端系统的API网关，提供统一的入口、路由、认证、限流、监控等功能。

## 技术栈

- **Spring Boot**: 3.2.5
- **Spring Cloud Gateway**: WebFlux响应式网关
- **Spring Cloud LoadBalancer**: 客户端负载均衡
- **Resilience4j**: 断路器和限流
- **Nacos**: 服务发现和配置中心
- **Redis**: 分布式限流和缓存
- **JWT**: Token认证

## 核心功能

### 1. 服务路由
- 用户服务: `/api/user/**` → `user-service`
- 认证服务: `/api/auth/**` → `auth-service`
- 连接服务: `/api/connection/**` → `connection-service`
- 消息服务: `/api/message/**` → `message-sequence-service`

### 2. 全局过滤器

#### JWT认证过滤器 (优先级: -100)
- 验证JWT Token的有效性
- 提取用户信息并传递给下游服务
- 跳过无需认证的路径（注册、登录、健康检查等）

#### 请求日志过滤器 (优先级: -200)
- 记录请求开始和结束时间
- 生成唯一请求ID用于链路追踪
- 记录客户端IP、请求方法、URI等信息

#### 限流过滤器 (优先级: -50)
- 基于Redis Lua脚本实现分布式限流
- 支持不同接口的差异化限流策略
- 返回限流状态响应头

### 3. 断路器配置
- 支持各服务独立的断路器配置
- 失败率阈值、慢调用阈值、熔断时间等可配置
- 提供降级处理和错误响应

### 4. 跨域配置
- 支持所有来源的跨域请求
- 配置常用HTTP方法和请求头
- 支持凭证传递

## 配置说明

### 限流策略
```yaml
/api/auth/ -> 10次/分钟     # 认证接口
/api/user/register -> 5次/分钟  # 注册接口
/api/user/send-verification-code -> 3次/分钟  # 验证码接口
/api/ -> 100次/分钟  # 其他API接口
```

### 断路器配置
- **失败率阈值**: 50%-60%
- **慢调用阈值**: 2-5秒
- **最小调用次数**: 3-5次
- **熔断时间**: 20-30秒

## 启动方式

### 1. 确保基础设施运行
```bash
./start-infrastructure.sh
```

### 2. 启动Gateway服务
```bash
mvn spring-boot:run -pl gateway
```

### 3. 验证服务状态
```bash
curl http://localhost:8080/actuator/health
```

## API文档

### 健康检查
- **URL**: `GET /actuator/health`
- **响应**: 服务状态和基本信息

### 降级接口
- **用户服务降级**: `/fallback/user`
- **认证服务降级**: `/fallback/auth`
- **连接服务降级**: `/fallback/connection`
- **消息服务降级**: `/fallback/message`

## 监控和日志

### 日志级别
- Gateway组件: DEBUG
- Web Reactive: DEBUG
- LoadBalancer: DEBUG

### 关键日志
- 请求开始/结束日志包含RequestId
- JWT认证成功/失败日志
- 限流触发日志
- 断路器状态变化日志

## 目录结构

```
gateway/
├── src/main/java/com/tencent/im/backend/gateway/
│   ├── GatewayApplication.java          # 主启动类
│   ├── config/
│   │   └── GatewayConfig.java          # 路由配置
│   ├── controller/
│   │   ├── FallbackController.java     # 降级处理
│   │   └── HealthController.java       # 健康检查
│   ├── exception/
│   │   └── GlobalExceptionHandler.java # 全局异常处理
│   └── filter/
│       ├── JwtAuthenticationFilter.java # JWT认证
│       ├── RateLimitFilter.java        # 限流过滤器
│       └── RequestLoggingFilter.java   # 请求日志
├── src/main/resources/
│   └── bootstrap.yml                   # 启动配置
└── pom.xml                            # Maven配置
```

## 部署注意事项

1. **Redis依赖**: 确保Redis服务可用，用于分布式限流
2. **Nacos依赖**: 确保Nacos服务注册中心可用
3. **JWT密钥**: 生产环境需要配置安全的JWT密钥
4. **限流策略**: 根据实际业务调整限流阈值
5. **断路器参数**: 根据下游服务特性调整断路器配置

## 故障排查

### 常见问题
1. **503 Service Unavailable**: 检查下游服务是否启动并注册到Nacos
2. **401 Unauthorized**: 检查JWT Token是否有效
3. **429 Too Many Requests**: 触发限流，检查请求频率
4. **504 Gateway Timeout**: 下游服务响应超时，检查断路器配置

### 调试方法
1. 查看Gateway日志中的RequestId进行链路追踪
2. 检查Nacos控制台中的服务注册状态
3. 检查Redis中的限流计数器
4. 查看Resilience4j断路器状态
