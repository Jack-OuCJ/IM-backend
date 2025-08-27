package com.im.redis.service;

import com.im.redis.config.RedisConfig;
import com.im.redis.util.RedisKeyUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis List Operations Service
 * Demonstrates List operations for message queues and chat histories
 */
@Slf4j
@Service
public class RedisListService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisKeyUtil redisKeyUtil;

    @Autowired
    private RedisConfig.TtlGenerator ttlGenerator;

    /**
     * Push element to the left of list (head)
     */
    public long leftPush(String module, String entity, String id, String value) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        long result = stringRedisTemplate.opsForList().leftPush(key, value);
        
        // Set TTL for new list
        if (result == 1) {
            long ttl = ttlGenerator.generateTtl();
            stringRedisTemplate.expire(key, Duration.ofSeconds(ttl));
            log.info("Set TTL {} seconds for new list key: {}", ttl, key);
        }
        
        log.info("Left push to list - key: {}, value: {}, new size: {}", key, value, result);
        return result;
    }

    /**
     * Push element to the right of list (tail)
     */
    public long rightPush(String module, String entity, String id, String value) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        long result = stringRedisTemplate.opsForList().rightPush(key, value);
        
        // Set TTL for new list
        if (result == 1) {
            long ttl = ttlGenerator.generateTtl();
            stringRedisTemplate.expire(key, Duration.ofSeconds(ttl));
            log.info("Set TTL {} seconds for new list key: {}", ttl, key);
        }
        
        log.info("Right push to list - key: {}, value: {}, new size: {}", key, value, result);
        return result;
    }

    /**
     * Push multiple elements to the right of list
     */
    public long rightPushAll(String module, String entity, String id, String... values) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        long result = stringRedisTemplate.opsForList().rightPushAll(key, values);
        
        // Set TTL for new list
        if (result == values.length) {
            long ttl = ttlGenerator.generateTtl();
            stringRedisTemplate.expire(key, Duration.ofSeconds(ttl));
            log.info("Set TTL {} seconds for new list key: {}", ttl, key);
        }
        
        log.info("Right push all to list - key: {}, values count: {}, new size: {}", 
                key, values.length, result);
        return result;
    }

    /**
     * Pop element from the left of list (head)
     */
    public String leftPop(String module, String entity, String id) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        String result = stringRedisTemplate.opsForList().leftPop(key);
        log.info("Left pop from list - key: {}, value: {}", key, result);
        return result;
    }

    /**
     * Pop element from the right of list (tail)
     */
    public String rightPop(String module, String entity, String id) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        String result = stringRedisTemplate.opsForList().rightPop(key);
        log.info("Right pop from list - key: {}, value: {}", key, result);
        return result;
    }

    /**
     * Blocking left pop with timeout
     */
    public String blockingLeftPop(String module, String entity, String id, long timeout, TimeUnit unit) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        String result = stringRedisTemplate.opsForList().leftPop(key, Duration.of(timeout, unit.toChronoUnit()));
        log.info("Blocking left pop from list - key: {}, timeout: {} {}, value: {}", 
                key, timeout, unit, result);
        return result;
    }

    /**
     * Get list element by index
     */
    public String getByIndex(String module, String entity, String id, long index) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        String result = stringRedisTemplate.opsForList().index(key, index);
        log.info("Get list element by index - key: {}, index: {}, value: {}", key, index, result);
        return result;
    }

    /**
     * Get list range
     */
    public List<String> getRange(String module, String entity, String id, long start, long end) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        List<String> result = stringRedisTemplate.opsForList().range(key, start, end);
        log.info("Get list range - key: {}, start: {}, end: {}, count: {}", 
                key, start, end, result != null ? result.size() : 0);
        return result;
    }

    /**
     * Get all list elements
     */
    public List<String> getAllElements(String module, String entity, String id) {
        return getRange(module, entity, id, 0, -1);
    }

    /**
     * Get list size
     */
    public long getSize(String module, String entity, String id) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        long size = stringRedisTemplate.opsForList().size(key);
        log.info("Get list size - key: {}, size: {}", key, size);
        return size;
    }

    /**
     * Set list element by index
     */
    public void setByIndex(String module, String entity, String id, long index, String value) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        stringRedisTemplate.opsForList().set(key, index, value);
        log.info("Set list element by index - key: {}, index: {}, value: {}", key, index, value);
    }

    /**
     * Remove elements from list
     */
    public long removeElements(String module, String entity, String id, long count, String value) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        long result = stringRedisTemplate.opsForList().remove(key, count, value);
        log.info("Remove elements from list - key: {}, count: {}, value: {}, removed: {}", 
                key, count, value, result);
        return result;
    }

    /**
     * Trim list to specified range
     */
    public void trimList(String module, String entity, String id, long start, long end) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        stringRedisTemplate.opsForList().trim(key, start, end);
        log.info("Trim list - key: {}, start: {}, end: {}", key, start, end);
    }

    /**
     * Keep only the latest N elements (useful for chat history)
     */
    public void keepLatest(String module, String entity, String id, long maxSize) {
        long currentSize = getSize(module, entity, id);
        if (currentSize > maxSize) {
            trimList(module, entity, id, -maxSize, -1);
            log.info("Trimmed list to keep latest {} elements - key: {}", 
                    maxSize, redisKeyUtil.buildKey(module, entity, id));
        }
    }

    /**
     * Move element from one list to another (atomic operation)
     */
    public String moveElement(String sourceModule, String sourceEntity, String sourceId,
                             String destModule, String destEntity, String destId) {
        String sourceKey = redisKeyUtil.buildKey(sourceModule, sourceEntity, sourceId);
        String destKey = redisKeyUtil.buildKey(destModule, destEntity, destId);
        
        String result = stringRedisTemplate.opsForList().rightPopAndLeftPush(sourceKey, destKey);
        log.info("Move element from {} to {} - value: {}", sourceKey, destKey, result);
        return result;
    }
}
