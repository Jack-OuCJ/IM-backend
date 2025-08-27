package com.im.redis.service;

import com.im.redis.util.RedisKeyUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Redis Pub/Sub Service
 * Demonstrates publish/subscribe patterns for real-time messaging
 */
@Slf4j
@Service
public class RedisPubSubService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisMessageListenerContainer messageListenerContainer;

    @Autowired
    private RedisKeyUtil redisKeyUtil;

    // Track active subscriptions
    private final Map<String, ChannelMessageListener> activeListeners = new ConcurrentHashMap<>();
    private final AtomicInteger messageCounter = new AtomicInteger(0);

    @PostConstruct
    public void init() {
        log.info("Redis Pub/Sub service initialized");
    }

    @PreDestroy
    public void cleanup() {
        // Remove all listeners
        activeListeners.forEach((channel, listener) -> {
            messageListenerContainer.removeMessageListener(listener);
            log.info("Removed listener for channel: {}", channel);
        });
        activeListeners.clear();
        log.info("Redis Pub/Sub service cleaned up");
    }

    /**
     * Publish message to channel
     */
    public void publishMessage(String channelType, String channelId, String message) {
        String channel = redisKeyUtil.buildChannelKey(channelType, channelId);
        stringRedisTemplate.convertAndSend(channel, message);
        log.info("Published message to channel: {} - message: {}", channel, message);
    }

    /**
     * Publish structured message with metadata
     */
    public void publishStructuredMessage(String channelType, String channelId, String sender, 
                                       String messageType, String content) {
        String channel = redisKeyUtil.buildChannelKey(channelType, channelId);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        int messageId = messageCounter.incrementAndGet();
        
        String structuredMessage = String.format("{\"id\":%d,\"sender\":\"%s\",\"type\":\"%s\",\"content\":\"%s\",\"timestamp\":\"%s\"}", 
                messageId, sender, messageType, content, timestamp);
        
        stringRedisTemplate.convertAndSend(channel, structuredMessage);
        log.info("Published structured message to channel: {} - id: {}, sender: {}, type: {}", 
                channel, messageId, sender, messageType);
    }

    /**
     * Subscribe to channel with custom message handler
     */
    public void subscribeToChannel(String channelType, String channelId, MessageHandler handler) {
        String channel = redisKeyUtil.buildChannelKey(channelType, channelId);
        
        // Remove existing listener if any
        unsubscribeFromChannel(channelType, channelId);
        
        ChannelMessageListener listener = new ChannelMessageListener(channel, handler);
        ChannelTopic topic = new ChannelTopic(channel);
        
        messageListenerContainer.addMessageListener(listener, topic);
        activeListeners.put(channel, listener);
        
        log.info("Subscribed to channel: {} with custom handler", channel);
    }

    /**
     * Subscribe to channel with default logging handler
     */
    public void subscribeToChannel(String channelType, String channelId) {
        subscribeToChannel(channelType, channelId, new DefaultMessageHandler());
    }

    /**
     * Unsubscribe from channel
     */
    public void unsubscribeFromChannel(String channelType, String channelId) {
        String channel = redisKeyUtil.buildChannelKey(channelType, channelId);
        ChannelMessageListener listener = activeListeners.remove(channel);
        
        if (listener != null) {
            messageListenerContainer.removeMessageListener(listener);
            log.info("Unsubscribed from channel: {}", channel);
        } else {
            log.warn("No active subscription found for channel: {}", channel);
        }
    }

    /**
     * Get active subscriptions count
     */
    public int getActiveSubscriptionsCount() {
        int count = activeListeners.size();
        log.info("Active subscriptions count: {}", count);
        return count;
    }

    /**
     * Broadcast message to all channels of a type
     */
    public void broadcastToChannelType(String channelType, String message) {
        String pattern = redisKeyUtil.buildChannelKey(channelType, "*");
        
        // For demonstration, we'll publish to some predefined channels
        // In real implementation, you might maintain a list of active channels
        for (int i = 1; i <= 3; i++) {
            publishMessage(channelType, "room" + i, message);
        }
        
        log.info("Broadcasted message to all channels of type: {} - message: {}", channelType, message);
    }

    /**
     * Publish chat message
     */
    public void publishChatMessage(String roomId, String userId, String message) {
        publishStructuredMessage("chat", roomId, userId, "message", message);
    }

    /**
     * Publish system notification
     */
    public void publishSystemNotification(String userId, String notification) {
        publishStructuredMessage("notification", userId, "system", "notification", notification);
    }

    /**
     * Publish user status update
     */
    public void publishUserStatusUpdate(String userId, String status) {
        publishStructuredMessage("status", "global", userId, "status_update", status);
    }

    /**
     * Subscribe to chat room
     */
    public void subscribeToChatRoom(String roomId, MessageHandler handler) {
        subscribeToChannel("chat", roomId, handler);
    }

    /**
     * Subscribe to user notifications
     */
    public void subscribeToUserNotifications(String userId, MessageHandler handler) {
        subscribeToChannel("notification", userId, handler);
    }

    /**
     * Subscribe to global user status updates
     */
    public void subscribeToUserStatusUpdates(MessageHandler handler) {
        subscribeToChannel("status", "global", handler);
    }

    /**
     * Message handler interface
     */
    public interface MessageHandler {
        void handleMessage(String channel, String message);
    }

    /**
     * Default message handler that logs messages
     */
    public static class DefaultMessageHandler implements MessageHandler {
        @Override
        public void handleMessage(String channel, String message) {
            log.info("Received message on channel: {} - message: {}", channel, message);
        }
    }

    /**
     * Custom message listener that wraps the handler
     */
    private static class ChannelMessageListener implements MessageListener {
        private final String channel;
        private final MessageHandler handler;

        public ChannelMessageListener(String channel, MessageHandler handler) {
            this.channel = channel;
            this.handler = handler;
        }

        @Override
        public void onMessage(Message message, byte[] pattern) {
            String messageContent = new String(message.getBody());
            try {
                handler.handleMessage(channel, messageContent);
            } catch (Exception e) {
                log.error("Error handling message on channel: {} - error: {}", channel, e.getMessage(), e);
            }
        }
    }

    /**
     * Demo chat room message handler
     */
    public static class ChatRoomMessageHandler implements MessageHandler {
        private final String roomId;

        public ChatRoomMessageHandler(String roomId) {
            this.roomId = roomId;
        }

        @Override
        public void handleMessage(String channel, String message) {
            log.info("📥 Chat Room [{}] received: {}", roomId, message);
            // In real implementation, you would:
            // 1. Parse the structured message
            // 2. Update UI in real-time
            // 3. Store message history
            // 4. Send push notifications if needed
        }
    }

    /**
     * Demo notification handler
     */
    public static class NotificationMessageHandler implements MessageHandler {
        private final String userId;

        public NotificationMessageHandler(String userId) {
            this.userId = userId;
        }

        @Override
        public void handleMessage(String channel, String message) {
            log.info("🔔 User [{}] notification: {}", userId, message);
            // In real implementation, you would:
            // 1. Parse notification data
            // 2. Show notification in UI
            // 3. Store in notification history
            // 4. Handle different notification types
        }
    }
}
