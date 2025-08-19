package com.tencent.im.backend.common.websocket.manager;

import com.tencent.im.backend.common.core.constant.IMConstants;
import com.tencent.im.backend.common.websocket.model.ConnectionInfo;
import org.redisson.api.RBucket;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * WebSocket连接管理器
 * 负责维护长连接、心跳检测、消息有序性保障
 */
@Component
public class ConnectionManager {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

    @Autowired
    private RedissonClient redissonClient;

    /** 本地连接缓存 */
    private final ConcurrentHashMap<String, WebSocketSession> localSessions = new ConcurrentHashMap<>();
    
    /** 本地连接信息缓存 */
    private final ConcurrentHashMap<String, ConnectionInfo> localConnections = new ConcurrentHashMap<>();

    /** Redis中的全局连接映射 */
    private static final String GLOBAL_CONNECTIONS_KEY = "im:connections";
    
    /** 用户在线状态前缀 */
    private static final String USER_ONLINE_PREFIX = "im:user:online:";

    /**
     * 添加连接
     */
    public void addConnection(String connectionId, String userId, WebSocketSession session) {
        ConnectionInfo connectionInfo = new ConnectionInfo(connectionId, userId);
        connectionInfo.setClientIp(getClientIp(session));
        connectionInfo.setUserAgent(getUserAgent(session));
        
        // 本地缓存
        localSessions.put(connectionId, session);
        localConnections.put(connectionId, connectionInfo);
        
        // 全局缓存到Redis
        RMap<String, ConnectionInfo> globalConnections = redissonClient.getMap(GLOBAL_CONNECTIONS_KEY);
        globalConnections.put(connectionId, connectionInfo);
        
        // 更新用户在线状态
        updateUserOnlineStatus(userId, true);
        
        logger.info("Connection added: connectionId={}, userId={}", connectionId, userId);
    }

    /**
     * 移除连接
     */
    public void removeConnection(String connectionId) {
        ConnectionInfo connectionInfo = localConnections.remove(connectionId);
        localSessions.remove(connectionId);
        
        if (connectionInfo != null) {
            // 从Redis移除
            RMap<String, ConnectionInfo> globalConnections = redissonClient.getMap(GLOBAL_CONNECTIONS_KEY);
            globalConnections.remove(connectionId);
            
            // 检查用户是否还有其他在线连接
            String userId = connectionInfo.getUserId();
            boolean hasOtherConnections = hasUserOnlineConnections(userId);
            if (!hasOtherConnections) {
                updateUserOnlineStatus(userId, false);
            }
            
            logger.info("Connection removed: connectionId={}, userId={}", connectionId, userId);
        }
    }

    /**
     * 获取本地连接
     */
    public WebSocketSession getLocalSession(String connectionId) {
        return localSessions.get(connectionId);
    }

    /**
     * 获取连接信息
     */
    public ConnectionInfo getConnectionInfo(String connectionId) {
        // 先检查本地缓存
        ConnectionInfo localInfo = localConnections.get(connectionId);
        if (localInfo != null) {
            return localInfo;
        }
        
        // 检查Redis
        RMap<String, ConnectionInfo> globalConnections = redissonClient.getMap(GLOBAL_CONNECTIONS_KEY);
        return globalConnections.get(connectionId);
    }

    /**
     * 更新连接活跃时间
     */
    public void updateConnectionActiveTime(String connectionId) {
        ConnectionInfo connectionInfo = localConnections.get(connectionId);
        if (connectionInfo != null) {
            connectionInfo.updateActiveTime();
            
            // 更新Redis中的信息
            RMap<String, ConnectionInfo> globalConnections = redissonClient.getMap(GLOBAL_CONNECTIONS_KEY);
            globalConnections.put(connectionId, connectionInfo);
        }
    }

    /**
     * 处理心跳
     */
    public void handleHeartbeat(String connectionId) {
        ConnectionInfo connectionInfo = localConnections.get(connectionId);
        if (connectionInfo != null) {
            connectionInfo.updateActiveTime();
            connectionInfo.incrementHeartbeat();
            
            // 更新Redis
            RMap<String, ConnectionInfo> globalConnections = redissonClient.getMap(GLOBAL_CONNECTIONS_KEY);
            globalConnections.put(connectionId, connectionInfo);
            
            logger.debug("Heartbeat received: connectionId={}, count={}", 
                        connectionId, connectionInfo.getHeartbeatCount().get());
        }
    }

    /**
     * 检查心跳超时的连接
     */
    public List<String> getTimeoutConnections() {
        LocalDateTime timeoutThreshold = LocalDateTime.now()
                .minusSeconds(IMConstants.Heartbeat.HEARTBEAT_TIMEOUT / 1000);
        
        return localConnections.values().stream()
                .filter(conn -> conn.getLastActiveTime().isBefore(timeoutThreshold))
                .map(ConnectionInfo::getConnectionId)
                .collect(Collectors.toList());
    }

    /**
     * 获取用户的所有连接
     */
    public List<String> getUserConnections(String userId) {
        RMap<String, ConnectionInfo> globalConnections = redissonClient.getMap(GLOBAL_CONNECTIONS_KEY);
        return globalConnections.values().stream()
                .filter(conn -> userId.equals(conn.getUserId()) && conn.isOnline())
                .map(ConnectionInfo::getConnectionId)
                .collect(Collectors.toList());
    }

    /**
     * 检查用户是否在线
     */
    public boolean isUserOnline(String userId) {
        RBucket<Boolean> userOnlineBucket = redissonClient.getBucket(USER_ONLINE_PREFIX + userId);
        Boolean online = userOnlineBucket.get();
        return online != null && online;
    }

    /**
     * 检查用户是否有在线连接
     */
    private boolean hasUserOnlineConnections(String userId) {
        return !getUserConnections(userId).isEmpty();
    }

    /**
     * 更新用户在线状态
     */
    private void updateUserOnlineStatus(String userId, boolean online) {
        RBucket<Boolean> userOnlineBucket = redissonClient.getBucket(USER_ONLINE_PREFIX + userId);
        if (online) {
            userOnlineBucket.set(true, 24, TimeUnit.HOURS);
        } else {
            userOnlineBucket.delete();
        }
        
        logger.info("User online status updated: userId={}, online={}", userId, online);
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(WebSocketSession session) {
        try {
            return session.getRemoteAddress() != null ? 
                   session.getRemoteAddress().getAddress().getHostAddress() : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * 获取用户代理
     */
    private String getUserAgent(WebSocketSession session) {
        try {
            List<String> userAgentHeaders = session.getHandshakeHeaders().get("User-Agent");
            return userAgentHeaders != null && !userAgentHeaders.isEmpty() ? 
                   userAgentHeaders.get(0) : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * 获取本地连接数量
     */
    public int getLocalConnectionCount() {
        return localConnections.size();
    }

    /**
     * 获取全局连接数量
     */
    public long getGlobalConnectionCount() {
        RMap<String, ConnectionInfo> globalConnections = redissonClient.getMap(GLOBAL_CONNECTIONS_KEY);
        return globalConnections.size();
    }
}
