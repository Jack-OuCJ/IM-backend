# User Service - 用户服务

## 概述

用户服务是IM后端系统的核心模块之一，主要负责用户注册、登录验证、用户信息管理等功能。该服务采用邮箱注册方式，支持邮箱验证码验证。

## 功能特性

### 用户注册
- 支持邮箱注册
- 邮箱验证码验证
- 用户名唯一性检查
- 密码加密存储（BCrypt）
- 用户ID自动生成（雪花算法）

### 邮箱验证
- 支持发送验证码到指定邮箱
- 验证码有效期5分钟
- 防重复发送机制（1分钟内限制）
- 支持多种用途（注册、找回密码、修改邮箱）

### 用户查询
- 根据用户名查询用户信息
- 根据邮箱查询用户信息
- 根据用户ID查询用户信息

## 技术栈

- **框架**: Spring Boot 3.2.0
- **数据库**: MySQL 8.0
- **ORM**: MyBatis Plus 3.5.5
- **连接池**: Druid 1.2.20
- **缓存**: Redis (通过Redisson)
- **服务发现**: Nacos
- **API文档**: Knife4j (Swagger)
- **邮件服务**: Spring Boot Mail
- **工具库**: Hutool

## 数据库设计

### 用户表 (users)
```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL UNIQUE COMMENT '用户ID',
    username VARCHAR(64) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    nickname VARCHAR(64) COMMENT '昵称',
    avatar_url VARCHAR(255) COMMENT '头像URL',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱',
    email_verified TINYINT NOT NULL DEFAULT 0 COMMENT '邮箱是否验证：1-已验证，0-未验证',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-正常，0-禁用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
);
```

### 邮箱验证表 (email_verifications)
```sql
CREATE TABLE email_verifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(100) NOT NULL COMMENT '邮箱地址',
    verification_code VARCHAR(10) NOT NULL COMMENT '验证码',
    purpose TINYINT NOT NULL COMMENT '用途：1-注册，2-找回密码，3-修改邮箱',
    expires_at TIMESTAMP NOT NULL COMMENT '过期时间',
    verified TINYINT NOT NULL DEFAULT 0 COMMENT '是否已验证：1-已验证，0-未验证',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
);
```

## API接口

### 发送验证码
```
POST /api/user/send-verification-code
Content-Type: application/json

{
    "email": "user@example.com",
    "purpose": 1
}
```

### 用户注册
```
POST /api/user/register
Content-Type: application/json

{
    "username": "testuser",
    "password": "123456",
    "confirmPassword": "123456",
    "email": "user@example.com",
    "verificationCode": "123456",
    "nickname": "测试用户"
}
```

### 查询用户信息
```
GET /api/user/username/{username}
GET /api/user/email/{email}
GET /api/user/{userId}
```

## 配置说明

### 应用配置 (application.yml)
```yaml
server:
  port: 8081

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/im_backend
    username: root
    password: 123456
  
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}

user:
  email:
    verification:
      expiration: 300 # 验证码过期时间（秒）
```

### Nacos配置 (bootstrap.yml)
```yaml
spring:
  application:
    name: user-service
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER_ADDR:127.0.0.1:8848}
      config:
        server-addr: ${NACOS_SERVER_ADDR:127.0.0.1:8848}
        file-extension: yml
```

## 项目结构

```
src/main/java/com/tencent/im/backend/user/
├── UserServiceApplication.java          # 启动类
├── config/                              # 配置类
│   ├── MybatisPlusConfig.java          # MyBatis Plus配置
│   └── SwaggerConfig.java              # Swagger配置
├── controller/                          # 控制器层
│   └── UserController.java            # 用户控制器
├── dto/                                # 数据传输对象
│   ├── SendVerificationCodeRequest.java
│   ├── UserRegisterRequest.java
│   └── UserResponse.java
├── entity/                             # 实体类
│   ├── User.java                       # 用户实体
│   └── EmailVerification.java         # 邮箱验证实体
├── mapper/                             # 数据访问层
│   ├── UserMapper.java
│   └── EmailVerificationMapper.java
└── service/                            # 服务层
    ├── UserService.java               # 用户服务接口
    ├── EmailService.java              # 邮件服务接口
    └── impl/                          # 服务实现
        ├── UserServiceImpl.java
        └── EmailServiceImpl.java
```

## 部署和运行

### 1. 环境准备
- JDK 17+
- MySQL 8.0+
- Redis
- Nacos

### 2. 数据库初始化
执行 `/docker/mysql/init/init.sql` 脚本创建数据库表。

### 3. 配置邮件服务
在环境变量或配置文件中设置：
```
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
```

### 4. 启动服务
```bash
cd services/user-service
mvn spring-boot:run
```

### 5. 访问API文档
服务启动后，访问 `http://localhost:8081/doc.html` 查看API文档。

## 注意事项

1. **邮件配置**：需要配置有效的SMTP服务器信息
2. **数据库连接**：确保MySQL服务正常运行
3. **Nacos服务**：确保Nacos注册中心正常运行
4. **Redis服务**：确保Redis缓存服务正常运行
5. **端口冲突**：默认端口8081，如有冲突请修改配置

## 开发说明

### 添加新功能
1. 在相应的包下创建新的类文件
2. 更新数据库表结构（如需要）
3. 编写相应的测试用例
4. 更新API文档

### 测试
```bash
# 运行单元测试
mvn test

# 运行特定测试
mvn test -Dtest=UserServiceTest
```

## 后续扩展

1. **密码找回功能**：基于邮箱验证码的密码重置
2. **手机号注册**：支持手机验证码注册
3. **第三方登录**：集成微信、QQ等第三方登录
4. **用户权限管理**：角色和权限控制
5. **用户资料完善**：头像上传、个人信息编辑
