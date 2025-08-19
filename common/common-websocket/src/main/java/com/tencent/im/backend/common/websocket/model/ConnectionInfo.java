package com.tencent.im.backend.common.websocket.model;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * WebSocket连接信息
 */
public class ConnectionInfo {

    /** 连接ID */
    private String connectionId;

    /** 用户ID */
    private String userId;

    /** 设备ID */
    private String deviceId;

    /** 设备类型 */
    private String deviceType;

    /** 连接时间 */
    private LocalDateTime connectTime;

    /** 最后活跃时间 */
    private volatile LocalDateTime lastActiveTime;

    /** 心跳次数 */
    private AtomicLong heartbeatCount = new AtomicLong(0);

    /** 是否在线 */
    private volatile boolean online = true;

    /** 客户端IP */
    private String clientIp;

    /** 用户代理 */
    private String userAgent;

    public ConnectionInfo(String connectionId, String userId) {
        this.connectionId = connectionId;
        this.userId = userId;
        this.connectTime = LocalDateTime.now();
        this.lastActiveTime = LocalDateTime.now();
    }

    /**
     * 更新最后活跃时间
     */
    public void updateActiveTime() {
        this.lastActiveTime = LocalDateTime.now();
    }

    /**
     * 增加心跳次数
     */
    public long incrementHeartbeat() {
        return heartbeatCount.incrementAndGet();
    }

    // Getters and Setters
    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public LocalDateTime getConnectTime() {
        return connectTime;
    }

    public void setConnectTime(LocalDateTime connectTime) {
        this.connectTime = connectTime;
    }

    public LocalDateTime getLastActiveTime() {
        return lastActiveTime;
    }

    public void setLastActiveTime(LocalDateTime lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }

    public AtomicLong getHeartbeatCount() {
        return heartbeatCount;
    }

    public void setHeartbeatCount(AtomicLong heartbeatCount) {
        this.heartbeatCount = heartbeatCount;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
}
