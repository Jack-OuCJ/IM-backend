package com.im.redis.service;

import com.im.redis.config.RedisConfig;
import com.im.redis.util.RedisKeyUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

/**
 * Redis Hash Operations Service
 * Demonstrates Hash operations with field-level management
 */
@Slf4j
@Service
public class RedisHashService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisKeyUtil redisKeyUtil;

    @Autowired
    private RedisConfig.TtlGenerator ttlGenerator;

    /**
     * Set hash field
     */
    public void setHashField(String module, String entity, String id, String field, String value) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        stringRedisTemplate.opsForHash().put(key, field, value);
        
        // Set TTL for the hash if it's new
        if (stringRedisTemplate.getExpire(key) == -1) {
            long ttl = ttlGenerator.generateTtl();
            stringRedisTemplate.expire(key, Duration.ofSeconds(ttl));
            log.info("Set TTL {} seconds for new hash key: {}", ttl, key);
        }
        
        log.info("Set hash field - key: {}, field: {}, value: {}", key, field, value);
    }

    /**
     * Get hash field
     */
    public String getHashField(String module, String entity, String id, String field) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        Object value = stringRedisTemplate.opsForHash().get(key, field);
        String result = value != null ? value.toString() : null;
        log.info("Get hash field - key: {}, field: {}, value: {}", key, field, result);
        return result;
    }

    /**
     * Set multiple hash fields
     */
    public void setHashFields(String module, String entity, String id, Map<String, String> fields) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        stringRedisTemplate.opsForHash().putAll(key, fields);
        
        // Set TTL for the hash if it's new
        if (stringRedisTemplate.getExpire(key) == -1) {
            long ttl = ttlGenerator.generateTtl();
            stringRedisTemplate.expire(key, Duration.ofSeconds(ttl));
            log.info("Set TTL {} seconds for new hash key: {}", ttl, key);
        }
        
        log.info("Set hash fields - key: {}, fields count: {}", key, fields.size());
    }

    /**
     * Get all hash fields
     */
    public Map<Object, Object> getAllHashFields(String module, String entity, String id) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        Map<Object, Object> result = stringRedisTemplate.opsForHash().entries(key);
        log.info("Get all hash fields - key: {}, fields count: {}", key, result.size());
        return result;
    }

    /**
     * Check if hash field exists
     */
    public boolean hasHashField(String module, String entity, String id, String field) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        boolean exists = stringRedisTemplate.opsForHash().hasKey(key, field);
        log.info("Hash field exists - key: {}, field: {}, exists: {}", key, field, exists);
        return exists;
    }

    /**
     * Delete hash field
     */
    public boolean deleteHashField(String module, String entity, String id, String field) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        Long result = stringRedisTemplate.opsForHash().delete(key, field);
        boolean deleted = result > 0;
        log.info("Delete hash field - key: {}, field: {}, deleted: {}", key, field, deleted);
        return deleted;
    }

    /**
     * Get all hash field names
     */
    public Set<Object> getHashKeys(String module, String entity, String id) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        Set<Object> keys = stringRedisTemplate.opsForHash().keys(key);
        log.info("Get hash keys - key: {}, keys count: {}", key, keys.size());
        return keys;
    }

    /**
     * Get hash size
     */
    public long getHashSize(String module, String entity, String id) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        long size = stringRedisTemplate.opsForHash().size(key);
        log.info("Get hash size - key: {}, size: {}", key, size);
        return size;
    }

    /**
     * Increment hash field (for numeric values)
     */
    public long incrementHashField(String module, String entity, String id, String field, long delta) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        long result = stringRedisTemplate.opsForHash().increment(key, field, delta);
        
        // Set TTL for the hash if it's new
        if (stringRedisTemplate.getExpire(key) == -1) {
            long ttl = ttlGenerator.generateTtl();
            stringRedisTemplate.expire(key, Duration.ofSeconds(ttl));
            log.info("Set TTL {} seconds for new hash key: {}", ttl, key);
        }
        
        log.info("Increment hash field - key: {}, field: {}, delta: {}, new value: {}", 
                key, field, delta, result);
        return result;
    }

    /**
     * Set hash field if not exists
     */
    public boolean setHashFieldIfAbsent(String module, String entity, String id, String field, String value) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        boolean result = stringRedisTemplate.opsForHash().putIfAbsent(key, field, value);
        
        if (result && stringRedisTemplate.getExpire(key) == -1) {
            long ttl = ttlGenerator.generateTtl();
            stringRedisTemplate.expire(key, Duration.ofSeconds(ttl));
            log.info("Set TTL {} seconds for new hash key: {}", ttl, key);
        }
        
        log.info("SetIfAbsent hash field - key: {}, field: {}, value: {}, result: {}", 
                key, field, value, result);
        return result;
    }
}
