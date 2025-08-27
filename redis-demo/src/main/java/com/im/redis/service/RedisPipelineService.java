package com.im.redis.service;

import com.im.redis.util.RedisKeyUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Redis Pipeline and Batch Operations Service
 * Demonstrates pipeline operations for high-performance batch processing
 */
@Slf4j
@Service
public class RedisPipelineService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisKeyUtil redisKeyUtil;

    /**
     * Execute multiple operations in pipeline for better performance
     */
    public List<Object> executePipeline(Map<String, String> keyValues, Map<String, Integer> expireSeconds) {
        log.info("Executing pipeline with {} operations", keyValues.size());
        
        List<Object> results = stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            // Set multiple key-value pairs
            keyValues.forEach((key, value) -> {
                connection.stringCommands().set(key.getBytes(), value.getBytes());
                
                // Set expiration if specified
                if (expireSeconds.containsKey(key)) {
                    connection.keyCommands().expire(key.getBytes(), expireSeconds.get(key));
                }
            });
            
            return null; // Return value is ignored in pipeline
        });
        
        log.info("Pipeline executed successfully, results count: {}", results.size());
        return results;
    }

    /**
     * Batch set multiple string values with pipeline
     */
    public void batchSetStrings(Map<String, String> moduleEntityIdToValue) {
        log.info("Batch setting {} string values", moduleEntityIdToValue.size());
        
        stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            moduleEntityIdToValue.forEach((key, value) -> {
                connection.stringCommands().set(key.getBytes(), value.getBytes());
            });
            return null;
        });
        
        log.info("Batch set strings completed");
    }

    /**
     * Batch get multiple string values with pipeline
     */
    public List<Object> batchGetStrings(List<String> keys) {
        log.info("Batch getting {} string values", keys.size());
        
        List<Object> results = stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            keys.forEach(key -> {
                connection.stringCommands().get(key.getBytes());
            });
            return null;
        });
        
        log.info("Batch get strings completed, results count: {}", results.size());
        return results;
    }

    /**
     * Batch increment counters with pipeline
     */
    public List<Object> batchIncrementCounters(List<String> counterKeys) {
        log.info("Batch incrementing {} counters", counterKeys.size());
        
        List<Object> results = stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            counterKeys.forEach(key -> {
                connection.stringCommands().incr(key.getBytes());
            });
            return null;
        });
        
        log.info("Batch increment counters completed, results count: {}", results.size());
        return results;
    }

    /**
     * Batch add to lists with pipeline
     */
    public void batchAddToLists(Map<String, List<String>> listData) {
        log.info("Batch adding to {} lists", listData.size());
        
        stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            listData.forEach((listKey, values) -> {
                values.forEach(value -> {
                    connection.listCommands().rPush(listKey.getBytes(), value.getBytes());
                });
            });
            return null;
        });
        
        log.info("Batch add to lists completed");
    }

    /**
     * Batch add to sets with pipeline
     */
    public void batchAddToSets(Map<String, List<String>> setData) {
        log.info("Batch adding to {} sets", setData.size());
        
        stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            setData.forEach((setKey, members) -> {
                byte[][] memberBytes = members.stream()
                    .map(String::getBytes)
                    .toArray(byte[][]::new);
                connection.setCommands().sAdd(setKey.getBytes(), memberBytes);
            });
            return null;
        });
        
        log.info("Batch add to sets completed");
    }

    /**
     * Batch hash operations with pipeline
     */
    public void batchHashOperations(Map<String, Map<String, String>> hashData) {
        log.info("Batch hash operations for {} hashes", hashData.size());
        
        stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            hashData.forEach((hashKey, fields) -> {
                fields.forEach((field, value) -> {
                    connection.hashCommands().hSet(hashKey.getBytes(), field.getBytes(), value.getBytes());
                });
            });
            return null;
        });
        
        log.info("Batch hash operations completed");
    }

    /**
     * Batch check key existence with pipeline
     */
    public List<Object> batchCheckExists(List<String> keys) {
        log.info("Batch checking existence of {} keys", keys.size());
        
        List<Object> results = stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            keys.forEach(key -> {
                connection.keyCommands().exists(key.getBytes());
            });
            return null;
        });
        
        log.info("Batch check exists completed, results count: {}", results.size());
        return results;
    }

    /**
     * Batch delete keys with pipeline
     */
    public List<Object> batchDeleteKeys(List<String> keys) {
        log.info("Batch deleting {} keys", keys.size());
        
        List<Object> results = stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            keys.forEach(key -> {
                connection.keyCommands().del(key.getBytes());
            });
            return null;
        });
        
        log.info("Batch delete keys completed, results count: {}", results.size());
        return results;
    }

    /**
     * Batch set expiration with pipeline
     */
    public void batchSetExpiration(Map<String, Integer> keyExpirations) {
        log.info("Batch setting expiration for {} keys", keyExpirations.size());
        
        stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            keyExpirations.forEach((key, seconds) -> {
                connection.keyCommands().expire(key.getBytes(), seconds);
            });
            return null;
        });
        
        log.info("Batch set expiration completed");
    }

    /**
     * Performance comparison: single operations vs pipeline
     */
    public void performanceComparison(Map<String, String> testData) {
        log.info("Starting performance comparison with {} operations", testData.size());
        
        // Single operations
        long startTime = System.currentTimeMillis();
        testData.forEach(stringRedisTemplate.opsForValue()::set);
        long singleOpsTime = System.currentTimeMillis() - startTime;
        
        // Pipeline operations
        startTime = System.currentTimeMillis();
        batchSetStrings(testData);
        long pipelineTime = System.currentTimeMillis() - startTime;
        
        log.info("Performance comparison - Single ops: {}ms, Pipeline: {}ms, Improvement: {}x", 
                singleOpsTime, pipelineTime, (double) singleOpsTime / pipelineTime);
        
        // Cleanup
        batchDeleteKeys(List.copyOf(testData.keySet()));
    }
}
