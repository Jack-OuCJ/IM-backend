# Auth Service 配置重构说明

## 重构概述

原来的 `AuthService` 使用 `@Value` 注解直接注入配置值，现在重构为使用 `@Component` + `@ConfigurationProperties` 的方式，提供更好的配置管理和类型安全。

## 主要变更

### 1. 新增配置属性类 `IMProperties`

```java
@Component
@ConfigurationProperties(prefix = "im")
public class IMProperties {
    private long sdkAppId;
    private UserSig userSig = new UserSig();
    private PrivateKey privateKey = new PrivateKey();
    
    public static class UserSig {
        private long expireSeconds = 86400; // 默认24小时
    }
    
    public static class PrivateKey {
        private String ref;
    }
}
```

### 2. 重构 `AuthService`

- 移除所有 `@Value` 注解
- 注入 `IMProperties` 配置类
- 通过配置类获取配置值

### 3. 更新 `ConfigController`

- 支持查看当前配置
- 支持运行时重新加载配置
- 提供健康检查端点

### 4. 新增配置刷新监听器

`ConfigRefreshListener` 监听 Nacos 配置变更事件，自动重新加载认证服务配置。

## 配置结构

### bootstrap.yml
```yaml
im:
  sdkAppId: 1400000000
  userSig:
    expireSeconds: 86400
  privateKey:
    ref: "file:///path/to/private_key.pem"
```

### Nacos 配置 (auth-service.yml)
```yaml
im:
  sdkAppId: 1400000000  # 你的实际SDKAppID
  userSig:
    expireSeconds: 86400  # UserSig有效期
  privateKey:
    ref: "file:///path/to/your/private_key.pem"  # 私钥文件路径
```

## API 端点

### 配置管理端点

1. **查看当前配置**
   ```
   GET /config/im-properties
   ```

2. **健康检查**
   ```
   GET /config/health
   ```

3. **重新加载配置**
   ```
   POST /config/reload
   ```

## 优势

1. **类型安全**: 配置属性有明确的类型定义
2. **IDE支持**: 更好的代码补全和重构支持
3. **配置验证**: 可以添加验证注解进行配置校验
4. **动态刷新**: 支持运行时配置更新
5. **结构化**: 配置具有清晰的层次结构
6. **可测试性**: 更容易进行单元测试

## 使用方法

### 开发环境

1. 启动 Nacos
2. 在 Nacos 中创建配置:
   - Data ID: `auth-service.yml`
   - Group: `DEFAULT_GROUP`
   - 配置内容: 参考 `nacos-config-example.yml`

3. 启动服务
4. 访问 `http://localhost:8089/config/im-properties` 验证配置加载

### 配置更新

1. 在 Nacos 控制台修改配置
2. 配置会自动刷新（通过 `ConfigRefreshListener`）
3. 或者手动调用 `POST /config/reload` 重新加载

### 监控

- 健康检查: `GET /config/health`
- 配置状态: `GET /config/im-properties`
- 应用监控: `GET /actuator/health`

## 注意事项

1. 确保 Nacos 配置中的 `sdkAppId` 和 `privateKey.ref` 正确设置
2. 私钥文件路径必须是服务器可访问的绝对路径
3. 配置变更后会自动重新加载，无需重启服务
4. 生产环境建议使用 Nacos 的命名空间和组进行环境隔离
