package com.im.redis.service;

import com.im.redis.util.RedisKeyUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * Redis Transaction and Optimistic Lock Service
 * Demonstrates transaction operations with optimistic locking using WATCH
 */
@Slf4j
@Service
public class RedisTransactionService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisKeyUtil redisKeyUtil;

    /**
     * Execute simple transaction
     */
    public List<Object> executeTransaction(String module, String entity, String id1, String value1,
                                          String id2, String value2) {
        String key1 = redisKeyUtil.buildKey(module, entity, id1);
        String key2 = redisKeyUtil.buildKey(module, entity, id2);
        
        log.info("Executing transaction for keys: {} and {}", key1, key2);
        
        // Execute transaction using SessionCallback to ensure proper transaction handling
        List<Object> results = stringRedisTemplate.execute(new org.springframework.data.redis.core.SessionCallback<List<Object>>() {
            @Override
            @SuppressWarnings("unchecked")
            public <K, V> List<Object> execute(@org.springframework.lang.NonNull org.springframework.data.redis.core.RedisOperations<K, V> operations) {
                operations.multi();
                
                // Queue operations
                ((org.springframework.data.redis.core.RedisOperations<String, String>) operations).opsForValue().set(key1, value1);
                ((org.springframework.data.redis.core.RedisOperations<String, String>) operations).opsForValue().set(key2, value2);
                ((org.springframework.data.redis.core.RedisOperations<String, String>) operations).expire(key1, Duration.ofMinutes(10));
                ((org.springframework.data.redis.core.RedisOperations<String, String>) operations).expire(key2, Duration.ofMinutes(10));
                
                // Execute transaction
                return operations.exec();
            }
        });
        
        log.info("Transaction executed successfully, operations count: {}", results != null ? results.size() : 0);
        return results;
    }

    /**
     * Optimistic lock with WATCH - Increment counter safely
     */
    public boolean incrementCounterWithOptimisticLock(String module, String entity, String id, int maxAttempts) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                List<Object> results = stringRedisTemplate.execute(new SessionCallback<List<Object>>() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public <K, V> List<Object> execute(@org.springframework.lang.NonNull org.springframework.data.redis.core.RedisOperations<K, V> operations) {
                        // Watch the key for changes
                        ((org.springframework.data.redis.core.RedisOperations<String, String>) operations).watch(key);
                        
                        // Get current value outside transaction
                        String currentValue = ((org.springframework.data.redis.core.RedisOperations<String, String>) operations).opsForValue().get(key);
                        int current = currentValue != null ? Integer.parseInt(currentValue) : 0;
                        
                        // Start transaction
                        operations.multi();
                        
                        // Queue increment operation
                        ((org.springframework.data.redis.core.RedisOperations<String, String>) operations).opsForValue().set(key, String.valueOf(current + 1));
                        ((org.springframework.data.redis.core.RedisOperations<String, String>) operations).expire(key, Duration.ofMinutes(10));
                        
                        // Execute transaction
                        return operations.exec();
                    }
                });
                
                if (results != null && !results.isEmpty()) {
                    log.info("Optimistic lock success on attempt {} - key: {}", attempt, key);
                    return true;
                } else {
                    log.warn("Optimistic lock failed on attempt {} - key: {}, retrying...", attempt, key);
                    // Transaction was discarded due to watched key modification
                    Thread.sleep(10 * attempt); // Exponential backoff
                }
                
            } catch (Exception e) {
                log.error("Error during optimistic lock attempt {} - key: {}, error: {}", 
                         attempt, key, e.getMessage());
                try {
                    Thread.sleep(10 * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        log.error("Optimistic lock failed after {} attempts - key: {}", maxAttempts, key);
        return false;
    }

    /**
     * Optimistic lock for transferring value between accounts
     */
    public boolean transferValue(String sourceModule, String sourceEntity, String sourceId,
                                String destModule, String destEntity, String destId,
                                int amount, int maxAttempts) {
        String sourceKey = redisKeyUtil.buildKey(sourceModule, sourceEntity, sourceId);
        String destKey = redisKeyUtil.buildKey(destModule, destEntity, destId);
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                List<Object> results = stringRedisTemplate.execute(new SessionCallback<List<Object>>() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public <K, V> List<Object> execute(@org.springframework.lang.NonNull org.springframework.data.redis.core.RedisOperations<K, V> operations) {
                        // Watch both keys
                        ((org.springframework.data.redis.core.RedisOperations<String, String>) operations).watch(Arrays.asList(sourceKey, destKey));
                        
                        // Get current values outside transaction
                        String sourceValue = ((org.springframework.data.redis.core.RedisOperations<String, String>) operations).opsForValue().get(sourceKey);
                        String destValue = ((org.springframework.data.redis.core.RedisOperations<String, String>) operations).opsForValue().get(destKey);
                        
                        int sourceBalance = sourceValue != null ? Integer.parseInt(sourceValue) : 0;
                        int destBalance = destValue != null ? Integer.parseInt(destValue) : 0;
                        
                        // Check if transfer is possible
                        if (sourceBalance < amount) {
                            log.warn("Insufficient balance for transfer - source: {}, balance: {}, amount: {}", 
                                    sourceKey, sourceBalance, amount);
                            return null; // Return null to indicate failure
                        }
                        
                        // Start transaction
                        operations.multi();
                        
                        // Queue transfer operations
                        ((org.springframework.data.redis.core.RedisOperations<String, String>) operations).opsForValue().set(sourceKey, String.valueOf(sourceBalance - amount));
                        ((org.springframework.data.redis.core.RedisOperations<String, String>) operations).opsForValue().set(destKey, String.valueOf(destBalance + amount));
                        ((org.springframework.data.redis.core.RedisOperations<String, String>) operations).expire(sourceKey, Duration.ofMinutes(30));
                        ((org.springframework.data.redis.core.RedisOperations<String, String>) operations).expire(destKey, Duration.ofMinutes(30));
                        
                        // Execute transaction
                        return operations.exec();
                    }
                });
                
                if (results != null && !results.isEmpty()) {
                    log.info("Transfer successful on attempt {} - from {} to {}, amount: {}", 
                            attempt, sourceKey, destKey, amount);
                    return true;
                } else {
                    log.warn("Transfer failed on attempt {} due to concurrent modification, retrying...", attempt);
                    Thread.sleep(20 * attempt); // Exponential backoff
                }
                
            } catch (Exception e) {
                log.error("Error during transfer attempt {} - error: {}", attempt, e.getMessage());
                try {
                    Thread.sleep(20 * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        log.error("Transfer failed after {} attempts", maxAttempts);
        return false;
    }

    /**
     * Optimistic lock for updating user session with version check
     */
    public boolean updateUserSession(String userId, String sessionData, String expectedVersion, int maxAttempts) {
        final String sessionKey = redisKeyUtil.buildUserSessionKey(userId);
        final String versionKey = redisKeyUtil.buildKey("user", "session", userId, "version");
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            final int currentAttempt = attempt;
            try {
                List<Object> results = stringRedisTemplate.execute(new SessionCallback<List<Object>>() {
                    @Override
                    public <K, V> List<Object> execute(org.springframework.data.redis.core.RedisOperations<K, V> operations) throws org.springframework.dao.DataAccessException {
                        // Watch both session and version keys
                        ((org.springframework.data.redis.core.RedisOperations<String, String>) operations).watch(Arrays.asList(sessionKey, versionKey));
                        
                        // Get current version
                        String currentVersion = ((org.springframework.data.redis.core.RedisOperations<String, String>) operations).opsForValue().get(versionKey);
                        
                        // Check version
                        if (currentVersion != null && !currentVersion.equals(expectedVersion)) {
                            log.warn("Version mismatch for user session - userId: {}, expected: {}, current: {}", 
                                    userId, expectedVersion, currentVersion);
                            ((org.springframework.data.redis.core.RedisOperations<String, String>) operations).unwatch();
                            return null; // Return null to indicate failure
                        }
                        
                        // Start transaction
                        operations.multi();
                        
                        // Generate new version
                        String newVersion = String.valueOf(System.currentTimeMillis());
                        
                        // Queue update operations
                        ((org.springframework.data.redis.core.RedisOperations<String, String>) operations).opsForValue().set(sessionKey, sessionData);
                        ((org.springframework.data.redis.core.RedisOperations<String, String>) operations).opsForValue().set(versionKey, newVersion);
                        ((org.springframework.data.redis.core.RedisOperations<String, String>) operations).expire(sessionKey, Duration.ofHours(24));
                        ((org.springframework.data.redis.core.RedisOperations<String, String>) operations).expire(versionKey, Duration.ofHours(24));
                        
                        // Execute transaction
                        List<Object> txResults = operations.exec();
                        
                        if (txResults != null && !txResults.isEmpty()) {
                            log.info("User session updated successfully on attempt {} - userId: {}, new version: {}", 
                                    currentAttempt, userId, newVersion);
                        } else {
                            log.warn("User session update failed on attempt {} due to concurrent modification", currentAttempt);
                        }
                        
                        return txResults;
                    }
                });
                
                if (results != null && !results.isEmpty()) {
                    return true;
                }
                
                Thread.sleep(15 * attempt);
                
            } catch (Exception e) {
                log.error("Error during user session update attempt {} - userId: {}, error: {}", 
                         attempt, userId, e.getMessage());
                try {
                    Thread.sleep(15 * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        log.error("User session update failed after {} attempts - userId: {}", maxAttempts, userId);
        return false;
    }

    /**
     * Get current user session with version
     */
    public SessionWithVersion getUserSessionWithVersion(String userId) {
        String sessionKey = redisKeyUtil.buildUserSessionKey(userId);
        String versionKey = redisKeyUtil.buildKey("user", "session", userId, "version");
        
        String sessionData = stringRedisTemplate.opsForValue().get(sessionKey);
        String version = stringRedisTemplate.opsForValue().get(versionKey);
        
        return new SessionWithVersion(sessionData, version);
    }

    /**
     * Data transfer object for session with version
     */
    public static class SessionWithVersion {
        public final String sessionData;
        public final String version;
        
        public SessionWithVersion(String sessionData, String version) {
            this.sessionData = sessionData;
            this.version = version;
        }
        
        @Override
        public String toString() {
            return String.format("SessionWithVersion{sessionData='%s', version='%s'}", sessionData, version);
        }
    }
}
