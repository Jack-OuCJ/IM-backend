package com.tencent.im.backend.common.websocket.scheduler;

import com.tencent.im.backend.common.core.constant.IMConstants;
import com.tencent.im.backend.common.websocket.manager.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

/**
 * 心跳检测调度器
 * 定期检查连接状态，清理超时连接
 */
@Component
public class HeartbeatScheduler {

    private static final Logger logger = LoggerFactory.getLogger(HeartbeatScheduler.class);

    @Autowired
    private ConnectionManager connectionManager;

    /**
     * 每30秒检查一次心跳超时的连接
     */
    @Scheduled(fixedRate = IMConstants.Heartbeat.HEARTBEAT_INTERVAL)
    public void checkHeartbeatTimeout() {
        try {
            List<String> timeoutConnections = connectionManager.getTimeoutConnections();
            
            if (!timeoutConnections.isEmpty()) {
                logger.info("Found {} timeout connections, cleaning up...", timeoutConnections.size());
                
                for (String connectionId : timeoutConnections) {
                    try {
                        // 关闭超时的WebSocket连接
                        WebSocketSession session = connectionManager.getLocalSession(connectionId);
                        if (session != null && session.isOpen()) {
                            session.close();
                        }
                        
                        // 移除连接信息
                        connectionManager.removeConnection(connectionId);
                        
                        logger.info("Timeout connection cleaned: connectionId={}", connectionId);
                    } catch (Exception e) {
                        logger.error("Error cleaning timeout connection: connectionId={}", connectionId, e);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error in heartbeat timeout check", e);
        }
    }

    /**
     * 每分钟输出连接统计信息
     */
    @Scheduled(fixedRate = 60000)
    public void logConnectionStats() {
        try {
            int localCount = connectionManager.getLocalConnectionCount();
            long globalCount = connectionManager.getGlobalConnectionCount();
            
            logger.info("Connection stats - Local: {}, Global: {}", localCount, globalCount);
        } catch (Exception e) {
            logger.error("Error logging connection stats", e);
        }
    }
}
