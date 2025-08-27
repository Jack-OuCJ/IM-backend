package com.im.redis.service;

import com.im.redis.util.RedisKeyUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redis Distributed Lock Service using Redisson
 * Demonstrates distributed locking patterns for resource synchronization
 */
@Slf4j
@Service
public class RedisDistributedLockService {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RedisKeyUtil redisKeyUtil;

    /**
     * Execute code with distributed lock
     */
    public <T> T executeWithLock(String resource, long waitTime, long leaseTime, TimeUnit unit, 
                                Supplier<T> task) {
        String lockKey = redisKeyUtil.buildLockKey(resource);
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            // Try to acquire lock with timeout
            if (lock.tryLock(waitTime, leaseTime, unit)) {
                log.info("Acquired distributed lock for resource: {}", resource);
                try {
                    return task.get();
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                        log.info("Released distributed lock for resource: {}", resource);
                    }
                }
            } else {
                log.warn("Failed to acquire distributed lock for resource: {} within {} {}", 
                        resource, waitTime, unit);
                throw new RuntimeException("Failed to acquire lock for resource: " + resource);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while waiting for lock on resource: {}", resource);
            throw new RuntimeException("Interrupted while waiting for lock", e);
        }
    }

    /**
     * Execute code with distributed lock (with default timeouts)
     */
    public <T> T executeWithLock(String resource, Supplier<T> task) {
        return executeWithLock(resource, 10, 30, TimeUnit.SECONDS, task);
    }

    /**
     * Execute void operation with distributed lock
     */
    public void executeWithLock(String resource, long waitTime, long leaseTime, TimeUnit unit, 
                               Runnable task) {
        executeWithLock(resource, waitTime, leaseTime, unit, () -> {
            task.run();
            return null;
        });
    }

    /**
     * Execute void operation with distributed lock (with default timeouts)
     */
    public void executeWithLock(String resource, Runnable task) {
        executeWithLock(resource, 10, 30, TimeUnit.SECONDS, task);
    }

    /**
     * Try to acquire lock without blocking
     */
    public boolean tryLock(String resource, long leaseTime, TimeUnit unit) {
        String lockKey = redisKeyUtil.buildLockKey(resource);
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            boolean acquired = lock.tryLock(0, leaseTime, unit);
            if (acquired) {
                log.info("Successfully acquired non-blocking lock for resource: {}", resource);
            } else {
                log.info("Failed to acquire non-blocking lock for resource: {}", resource);
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while trying to acquire lock for resource: {}", resource);
            return false;
        }
    }

    /**
     * Manual lock management - acquire lock
     */
    public RLock acquireLock(String resource, long waitTime, long leaseTime, TimeUnit unit) {
        String lockKey = redisKeyUtil.buildLockKey(resource);
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            if (lock.tryLock(waitTime, leaseTime, unit)) {
                log.info("Manually acquired lock for resource: {}", resource);
                return lock;
            } else {
                log.warn("Failed to manually acquire lock for resource: {}", resource);
                return null;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while manually acquiring lock for resource: {}", resource);
            return null;
        }
    }

    /**
     * Manual lock management - release lock
     */
    public void releaseLock(RLock lock, String resource) {
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.info("Manually released lock for resource: {}", resource);
        } else {
            log.warn("Attempted to release lock not held by current thread for resource: {}", resource);
        }
    }

    /**
     * Check if resource is locked
     */
    public boolean isLocked(String resource) {
        String lockKey = redisKeyUtil.buildLockKey(resource);
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = lock.isLocked();
        log.info("Resource {} lock status: {}", resource, locked ? "LOCKED" : "UNLOCKED");
        return locked;
    }

    /**
     * Force unlock (use with caution)
     */
    public boolean forceUnlock(String resource) {
        String lockKey = redisKeyUtil.buildLockKey(resource);
        RLock lock = redissonClient.getLock(lockKey);
        boolean result = lock.forceUnlock();
        log.warn("Force unlocked resource: {}, result: {}", resource, result);
        return result;
    }

    /**
     * Example: Distributed counter with lock
     */
    public int incrementDistributedCounter(String counterId, int maxRetries) {
        String resource = "counter:" + counterId;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return executeWithLock(resource, 5, 10, TimeUnit.SECONDS, () -> {
                    // Simulate counter increment logic
                    String counterKey = redisKeyUtil.buildKey("system", "counter", counterId);
                    
                    // Get current value (this would typically be from a database or Redis)
                    // For demo purposes, we'll simulate this
                    int currentValue = getCurrentCounterValue(counterKey);
                    int newValue = currentValue + 1;
                    
                    // Update counter (this would typically update database or Redis)
                    updateCounterValue(counterKey, newValue);
                    
                    log.info("Incremented distributed counter {} from {} to {}", counterId, currentValue, newValue);
                    return newValue;
                });
                
            } catch (Exception e) {
                log.warn("Failed to increment counter on attempt {} - counterId: {}, error: {}", 
                        attempt, counterId, e.getMessage());
                
                if (attempt == maxRetries) {
                    throw new RuntimeException("Failed to increment counter after " + maxRetries + " attempts", e);
                }
                
                try {
                    Thread.sleep(100 * attempt); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during retry", ie);
                }
            }
        }
        
        throw new RuntimeException("This should never be reached");
    }

    /**
     * Example: Distributed resource allocation
     */
    public boolean allocateResource(String resourceType, String resourceId, String userId) {
        String resource = "allocation:" + resourceType + ":" + resourceId;
        
        return executeWithLock(resource, 3, 60, TimeUnit.SECONDS, () -> {
            // Check if resource is already allocated
            String allocationKey = redisKeyUtil.buildKey("allocation", resourceType, resourceId);
            String currentOwner = getCurrentResourceOwner(allocationKey);
            
            if (currentOwner != null) {
                log.info("Resource already allocated - type: {}, id: {}, owner: {}", 
                        resourceType, resourceId, currentOwner);
                return false;
            }
            
            // Allocate resource
            setResourceOwner(allocationKey, userId);
            log.info("Resource allocated successfully - type: {}, id: {}, owner: {}", 
                    resourceType, resourceId, userId);
            return true;
        });
    }

    // Helper methods for demo purposes
    private int getCurrentCounterValue(String counterKey) {
        // In real implementation, this would get value from Redis or database
        return (int) (Math.random() * 100);
    }

    private void updateCounterValue(String counterKey, int value) {
        // In real implementation, this would update Redis or database
        log.debug("Updated counter {} to value {}", counterKey, value);
    }

    private String getCurrentResourceOwner(String allocationKey) {
        // In real implementation, this would get from Redis or database
        return Math.random() > 0.7 ? "existing_user" : null;
    }

    private void setResourceOwner(String allocationKey, String userId) {
        // In real implementation, this would set in Redis or database
        log.debug("Set resource owner for {} to {}", allocationKey, userId);
    }
}
