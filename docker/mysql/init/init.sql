CREATE DATABASE IF NOT EXISTS im_backend DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE im_backend;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL UNIQUE COMMENT '用户ID',
    username VARCHAR(64) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    nickname VARCHAR(64) COMMENT '昵称',
    avatar_url VARCHAR(255) COMMENT '头像URL',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-正常，0-禁用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_username (username),
    INDEX idx_phone (phone),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 用户关系表
CREATE TABLE IF NOT EXISTS user_relationships (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    friend_user_id VARCHAR(64) NOT NULL COMMENT '好友用户ID',
    relationship_type TINYINT NOT NULL COMMENT '关系类型：1-好友，2-黑名单',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-正常，0-删除',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_friend (user_id, friend_user_id),
    INDEX idx_user_id (user_id),
    INDEX idx_friend_user_id (friend_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户关系表';

-- 群组表
CREATE TABLE IF NOT EXISTS groups (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id VARCHAR(64) NOT NULL UNIQUE COMMENT '群组ID',
    group_name VARCHAR(100) NOT NULL COMMENT '群组名称',
    group_type VARCHAR(20) NOT NULL COMMENT '群组类型',
    owner_user_id VARCHAR(64) NOT NULL COMMENT '群主用户ID',
    description TEXT COMMENT '群组描述',
    avatar_url VARCHAR(255) COMMENT '群头像URL',
    max_member_count INT NOT NULL DEFAULT 200 COMMENT '最大成员数',
    current_member_count INT NOT NULL DEFAULT 0 COMMENT '当前成员数',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-正常，0-解散',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_group_id (group_id),
    INDEX idx_owner_user_id (owner_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群组表';

-- 群成员表
CREATE TABLE IF NOT EXISTS group_members (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id VARCHAR(64) NOT NULL COMMENT '群组ID',
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    role TINYINT NOT NULL DEFAULT 1 COMMENT '角色：1-普通成员，2-管理员，3-群主',
    join_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-正常，0-退出',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_group_user (group_id, user_id),
    INDEX idx_group_id (group_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群成员表';

-- 消息序列号表
CREATE TABLE IF NOT EXISTS message_sequences (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sequence_key VARCHAR(255) NOT NULL UNIQUE COMMENT '序列号key',
    current_sequence BIGINT NOT NULL DEFAULT 0 COMMENT '当前序列号',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_sequence_key (sequence_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息序列号表';

-- 操作日志表
CREATE TABLE IF NOT EXISTS operation_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) COMMENT '操作用户ID',
    operation_type VARCHAR(50) NOT NULL COMMENT '操作类型',
    operation_desc TEXT COMMENT '操作描述',
    request_data TEXT COMMENT '请求数据',
    response_data TEXT COMMENT '响应数据',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    user_agent VARCHAR(500) COMMENT '用户代理',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_operation_type (operation_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 插入测试数据
INSERT INTO users (user_id, username, password, nickname) VALUES 
('user001', 'testuser1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iYqiSfFVMLVZqpjxSOpWOF5esQjG', '测试用户1'),
('user002', 'testuser2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iYqiSfFVMLVZqpjxSOpWOF5esQjG', '测试用户2'),
('user003', 'testuser3', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iYqiSfFVMLVZqpjxSOpWOF5esQjG', '测试用户3');

INSERT INTO groups (group_id, group_name, group_type, owner_user_id, description) VALUES 
('group001', '测试群组1', 'Public', 'user001', '这是一个测试群组');

INSERT INTO group_members (group_id, user_id, role) VALUES 
('group001', 'user001', 3),
('group001', 'user002', 1),
('group001', 'user003', 1);
