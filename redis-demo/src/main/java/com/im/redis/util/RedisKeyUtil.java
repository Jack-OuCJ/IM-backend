package com.im.redis.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RedisKeyUtil {
    
    @Value("${redis.key.prefix:im:demo}")
    private String keyPrefix;

    /**
     * Build Redis key with business prefix and hierarchy
     * Format: prefix:module:entity:id[:field]
     */
    public String buildKey(String module, String entity, String id) {
        return String.format("%s:%s:%s:%s", keyPrefix, module, entity, id);
    }

    /**
     * Build Redis key with field
     */
    public String buildKey(String module, String entity, String id, String field) {
        return String.format("%s:%s:%s:%s:%s", keyPrefix, module, entity, id, field);
    }

    /**
     * Build hash key for user session
     */
    public String buildUserSessionKey(String userId) {
        return buildKey("user", "session", userId);
    }

    /**
     * Build key for user profile
     */
    public String buildUserProfileKey(String userId) {
        return buildKey("user", "profile", userId);
    }

    /**
     * Build key for chat message list
     */
    public String buildChatMessageListKey(String chatId) {
        return buildKey("chat", "messages", chatId);
    }

    /**
     * Build key for online users set
     */
    public String buildOnlineUsersKey() {
        return buildKey("system", "online", "users");
    }

    /**
     * Build key for user ranking
     */
    public String buildUserRankingKey(String type) {
        return buildKey("ranking", "user", type);
    }

    /**
     * Build key for distributed lock
     */
    public String buildLockKey(String resource) {
        return buildKey("lock", "resource", resource);
    }

    /**
     * Build key for pub/sub channel
     */
    public String buildChannelKey(String channelType, String channelId) {
        return buildKey("channel", channelType, channelId);
    }

    /**
     * Build key for rate limiting
     */
    public String buildRateLimitKey(String userId, String action) {
        return buildKey("ratelimit", action, userId);
    }
}
