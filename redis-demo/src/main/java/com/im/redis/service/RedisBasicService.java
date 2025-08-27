package com.im.redis.service;

import com.im.redis.config.RedisConfig;
import com.im.redis.util.RedisKeyUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Redis Basic Operations Service
 * Demonstrates SET/GET/EXPIRE/DEL operations with best practices
 */
@Slf4j
@Service
public class RedisBasicService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisKeyUtil redisKeyUtil;

    @Autowired
    private RedisConfig.TtlGenerator ttlGenerator;

    /**
     * Set string value with TTL and jitter
     */
    public void setValue(String module, String entity, String id, String value) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        long ttl = ttlGenerator.generateTtl();
        
        stringRedisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttl));
        log.info("Set key: {} with value: {} and TTL: {} seconds", key, value, ttl);
    }

    /**
     * Set string value with custom TTL
     */
    public void setValue(String module, String entity, String id, String value, int customTtl) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        long ttl = ttlGenerator.generateTtl(customTtl);
        
        stringRedisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttl));
        log.info("Set key: {} with value: {} and custom TTL: {} seconds", key, value, ttl);
    }

    /**
     * Get string value
     */
    public String getValue(String module, String entity, String id) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        String value = stringRedisTemplate.opsForValue().get(key);
        log.info("Get key: {} returned value: {}", key, value);
        return value;
    }

    /**
     * Set expiration time for existing key
     */
    public boolean setExpire(String module, String entity, String id) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        long ttl = ttlGenerator.generateTtl();
        
        boolean result = stringRedisTemplate.expire(key, Duration.ofSeconds(ttl));
        log.info("Set expire for key: {} with TTL: {} seconds, result: {}", key, ttl, result);
        return result;
    }

    /**
     * Set custom expiration time
     */
    public boolean setExpire(String module, String entity, String id, int seconds) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        long ttl = ttlGenerator.generateTtl(seconds);
        
        boolean result = stringRedisTemplate.expire(key, Duration.ofSeconds(ttl));
        log.info("Set custom expire for key: {} with TTL: {} seconds, result: {}", key, ttl, result);
        return result;
    }

    /**
     * Get TTL of a key
     */
    public long getTtl(String module, String entity, String id) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        long ttl = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
        log.info("TTL for key: {} is {} seconds", key, ttl);
        return ttl;
    }

    /**
     * Delete key
     */
    public boolean deleteKey(String module, String entity, String id) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        Boolean result = stringRedisTemplate.delete(key);
        log.info("Delete key: {}, result: {}", key, result);
        return Boolean.TRUE.equals(result);
    }

    /**
     * Check if key exists
     */
    public boolean hasKey(String module, String entity, String id) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        Boolean exists = stringRedisTemplate.hasKey(key);
        log.info("Key: {} exists: {}", key, exists);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * Set if not exists (SETNX)
     */
    public boolean setIfAbsent(String module, String entity, String id, String value) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        long ttl = ttlGenerator.generateTtl();
        
        Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(key, value, Duration.ofSeconds(ttl));
        log.info("SetIfAbsent key: {} with value: {}, result: {}", key, value, result);
        return Boolean.TRUE.equals(result);
    }

    /**
     * Increment counter
     */
    public long increment(String module, String entity, String id) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        long result = stringRedisTemplate.opsForValue().increment(key);
        
        // Set TTL for new counter
        if (result == 1) {
            setExpire(module, entity, id);
        }
        
        log.info("Increment key: {}, new value: {}", key, result);
        return result;
    }

    /**
     * Increment by specific value
     */
    public long incrementBy(String module, String entity, String id, long delta) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        long result = stringRedisTemplate.opsForValue().increment(key, delta);
        
        // Set TTL for new counter
        if (result == delta) {
            setExpire(module, entity, id);
        }
        
        log.info("IncrementBy key: {} by {}, new value: {}", key, delta, result);
        return result;
    }
}
