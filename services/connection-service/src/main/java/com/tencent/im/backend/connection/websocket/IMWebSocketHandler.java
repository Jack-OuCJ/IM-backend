package com.tencent.im.backend.connection.websocket;

import com.alibaba.fastjson2.JSON;
import com.tencent.im.backend.common.websocket.manager.ConnectionManager;
import com.tencent.im.backend.common.websocket.model.ConnectionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.util.Map;
import java.util.UUID;

/**
 * WebSocket处理器
 * 处理长连接建立、心跳检测、消息收发
 */
@Component
public class IMWebSocketHandler implements WebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(IMWebSocketHandler.class);

    @Autowired
    private ConnectionManager connectionManager;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String connectionId = UUID.randomUUID().toString();
        String userId = getUserIdFromSession(session);
        
        if (userId == null) {
            logger.warn("No userId found in session, closing connection: sessionId={}", session.getId());
            session.close(CloseStatus.BAD_DATA.withReason("Missing userId"));
            return;
        }

        // 添加连接到管理器
        connectionManager.addConnection(connectionId, userId, session);
        
        // 将连接ID存储到session属性中
        session.getAttributes().put("connectionId", connectionId);
        session.getAttributes().put("userId", userId);

        // 发送连接成功消息
        WebSocketMessage connectMessage = new WebSocketMessage();
        connectMessage.setType("connect");
        connectMessage.setData(Map.of("connectionId", connectionId, "status", "connected"));
        session.sendMessage(new TextMessage(JSON.toJSONString(connectMessage)));

        logger.info("WebSocket connection established: connectionId={}, userId={}, sessionId={}", 
                   connectionId, userId, session.getId());
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String connectionId = (String) session.getAttributes().get("connectionId");
        String userId = (String) session.getAttributes().get("userId");

        if (connectionId == null || userId == null) {
            logger.warn("Invalid session attributes, closing connection: sessionId={}", session.getId());
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        // 更新连接活跃时间
        connectionManager.updateConnectionActiveTime(connectionId);

        if (message instanceof TextMessage) {
            String payload = ((TextMessage) message).getPayload();
            handleTextMessage(session, connectionId, userId, payload);
        } else if (message instanceof BinaryMessage) {
            handleBinaryMessage(session, connectionId, userId, (BinaryMessage) message);
        } else if (message instanceof PongMessage) {
            handlePongMessage(session, connectionId, userId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String connectionId = (String) session.getAttributes().get("connectionId");
        String userId = (String) session.getAttributes().get("userId");
        
        logger.error("WebSocket transport error: connectionId={}, userId={}, sessionId={}", 
                    connectionId, userId, session.getId(), exception);
        
        // 清理连接
        if (connectionId != null) {
            connectionManager.removeConnection(connectionId);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String connectionId = (String) session.getAttributes().get("connectionId");
        String userId = (String) session.getAttributes().get("userId");
        
        logger.info("WebSocket connection closed: connectionId={}, userId={}, sessionId={}, status={}", 
                   connectionId, userId, session.getId(), closeStatus);
        
        // 清理连接
        if (connectionId != null) {
            connectionManager.removeConnection(connectionId);
        }
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * 处理文本消息
     */
    private void handleTextMessage(WebSocketSession session, String connectionId, String userId, String payload) {
        try {
            WebSocketMessage message = JSON.parseObject(payload, WebSocketMessage.class);
            
            switch (message.getType()) {
                case "heartbeat":
                    handleHeartbeat(session, connectionId);
                    break;
                case "message":
                    handleChatMessage(session, connectionId, userId, message);
                    break;
                default:
                    logger.warn("Unknown message type: {}, connectionId={}", message.getType(), connectionId);
            }
        } catch (Exception e) {
            logger.error("Error handling text message: connectionId={}, payload={}", connectionId, payload, e);
        }
    }

    /**
     * 处理二进制消息
     */
    private void handleBinaryMessage(WebSocketSession session, String connectionId, String userId, BinaryMessage message) {
        logger.debug("Binary message received: connectionId={}, size={}", connectionId, message.getPayloadLength());
        // 处理二进制消息逻辑
    }

    /**
     * 处理Pong消息
     */
    private void handlePongMessage(WebSocketSession session, String connectionId, String userId) {
        logger.debug("Pong message received: connectionId={}", connectionId);
        connectionManager.handleHeartbeat(connectionId);
    }

    /**
     * 处理心跳消息
     */
    private void handleHeartbeat(WebSocketSession session, String connectionId) {
        try {
            // 更新心跳时间
            connectionManager.handleHeartbeat(connectionId);
            
            // 回复心跳响应
            WebSocketMessage response = new WebSocketMessage();
            response.setType("heartbeat_ack");
            response.setData(Map.of("timestamp", System.currentTimeMillis()));
            
            session.sendMessage(new TextMessage(JSON.toJSONString(response)));
            logger.debug("Heartbeat processed: connectionId={}", connectionId);
        } catch (Exception e) {
            logger.error("Error processing heartbeat: connectionId={}", connectionId, e);
        }
    }

    /**
     * 处理聊天消息
     */
    private void handleChatMessage(WebSocketSession session, String connectionId, String userId, WebSocketMessage message) {
        try {
            logger.info("Chat message received: connectionId={}, userId={}, message={}", 
                       connectionId, userId, message);
            
            // 这里可以转发到消息处理服务
            // 暂时只回复确认消息
            WebSocketMessage response = new WebSocketMessage();
            response.setType("message_ack");
            response.setData(Map.of("messageId", message.getData(), "status", "received"));
            
            session.sendMessage(new TextMessage(JSON.toJSONString(response)));
        } catch (Exception e) {
            logger.error("Error processing chat message: connectionId={}", connectionId, e);
        }
    }

    /**
     * 从session中获取用户ID
     */
    private String getUserIdFromSession(WebSocketSession session) {
        // 从查询参数中获取userId
        String query = session.getUri().getQuery();
        if (query != null) {
            String[] params = query.split("&");
            for (String param : params) {
                String[] kv = param.split("=");
                if (kv.length == 2 && "userId".equals(kv[0])) {
                    return kv[1];
                }
            }
        }
        return null;
    }

    /**
     * WebSocket消息模型
     */
    public static class WebSocketMessage {
        private String type;
        private Object data;
        private Long timestamp;

        public WebSocketMessage() {
            this.timestamp = System.currentTimeMillis();
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Object getData() {
            return data;
        }

        public void setData(Object data) {
            this.data = data;
        }

        public Long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Long timestamp) {
            this.timestamp = timestamp;
        }
    }
}
