package com.im.redis.service;

import com.im.redis.config.RedisConfig;
import com.im.redis.util.RedisKeyUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * Redis Set Operations Service
 * Demonstrates Set operations for unique collections and relationships
 */
@Slf4j
@Service
public class RedisSetService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisKeyUtil redisKeyUtil;

    @Autowired
    private RedisConfig.TtlGenerator ttlGenerator;

    /**
     * Add member to set
     */
    public long addMember(String module, String entity, String id, String member) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        long result = stringRedisTemplate.opsForSet().add(key, member);
        
        // Set TTL for new set
        if (stringRedisTemplate.opsForSet().size(key) == 1) {
            long ttl = ttlGenerator.generateTtl();
            stringRedisTemplate.expire(key, Duration.ofSeconds(ttl));
            log.info("Set TTL {} seconds for new set key: {}", ttl, key);
        }
        
        log.info("Add member to set - key: {}, member: {}, added: {}", key, member, result > 0);
        return result;
    }

    /**
     * Add multiple members to set
     */
    public long addMembers(String module, String entity, String id, String... members) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        long result = stringRedisTemplate.opsForSet().add(key, members);
        
        // Set TTL for new set
        if (stringRedisTemplate.opsForSet().size(key) == result) {
            long ttl = ttlGenerator.generateTtl();
            stringRedisTemplate.expire(key, Duration.ofSeconds(ttl));
            log.info("Set TTL {} seconds for new set key: {}", ttl, key);
        }
        
        log.info("Add members to set - key: {}, members count: {}, added count: {}", 
                key, members.length, result);
        return result;
    }

    /**
     * Remove member from set
     */
    public long removeMember(String module, String entity, String id, String member) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        long result = stringRedisTemplate.opsForSet().remove(key, member);
        log.info("Remove member from set - key: {}, member: {}, removed: {}", key, member, result > 0);
        return result;
    }

    /**
     * Remove multiple members from set
     */
    public long removeMembers(String module, String entity, String id, String... members) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        long result = stringRedisTemplate.opsForSet().remove(key, (Object[]) members);
        log.info("Remove members from set - key: {}, members count: {}, removed count: {}", 
                key, members.length, result);
        return result;
    }

    /**
     * Check if member exists in set
     */
    public boolean isMember(String module, String entity, String id, String member) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        boolean exists = stringRedisTemplate.opsForSet().isMember(key, member);
        log.info("Check member in set - key: {}, member: {}, exists: {}", key, member, exists);
        return exists;
    }

    /**
     * Get all members of set
     */
    public Set<String> getAllMembers(String module, String entity, String id) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        Set<String> members = stringRedisTemplate.opsForSet().members(key);
        log.info("Get all members - key: {}, count: {}", key, members != null ? members.size() : 0);
        return members;
    }

    /**
     * Get random member from set
     */
    public String getRandomMember(String module, String entity, String id) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        String member = stringRedisTemplate.opsForSet().randomMember(key);
        log.info("Get random member - key: {}, member: {}", key, member);
        return member;
    }

    /**
     * Get multiple random members from set
     */
    public List<String> getRandomMembers(String module, String entity, String id, long count) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        List<String> members = stringRedisTemplate.opsForSet().randomMembers(key, count);
        log.info("Get random members - key: {}, count: {}, result count: {}", 
                key, count, members != null ? members.size() : 0);
        return members;
    }

    /**
     * Pop random member from set
     */
    public String popRandomMember(String module, String entity, String id) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        String member = stringRedisTemplate.opsForSet().pop(key);
        log.info("Pop random member - key: {}, member: {}", key, member);
        return member;
    }

    /**
     * Get set size
     */
    public long getSize(String module, String entity, String id) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        long size = stringRedisTemplate.opsForSet().size(key);
        log.info("Get set size - key: {}, size: {}", key, size);
        return size;
    }

    /**
     * Move member from one set to another
     */
    public boolean moveMember(String sourceModule, String sourceEntity, String sourceId,
                             String destModule, String destEntity, String destId, String member) {
        String sourceKey = redisKeyUtil.buildKey(sourceModule, sourceEntity, sourceId);
        String destKey = redisKeyUtil.buildKey(destModule, destEntity, destId);
        
        boolean result = stringRedisTemplate.opsForSet().move(sourceKey, member, destKey);
        log.info("Move member from {} to {} - member: {}, moved: {}", sourceKey, destKey, member, result);
        return result;
    }

    /**
     * Get intersection of multiple sets
     */
    public Set<String> getIntersection(String module1, String entity1, String id1,
                                      String module2, String entity2, String id2) {
        String key1 = redisKeyUtil.buildKey(module1, entity1, id1);
        String key2 = redisKeyUtil.buildKey(module2, entity2, id2);
        
        Set<String> intersection = stringRedisTemplate.opsForSet().intersect(key1, key2);
        log.info("Get intersection of {} and {} - count: {}", 
                key1, key2, intersection != null ? intersection.size() : 0);
        return intersection;
    }

    /**
     * Get union of multiple sets
     */
    public Set<String> getUnion(String module1, String entity1, String id1,
                               String module2, String entity2, String id2) {
        String key1 = redisKeyUtil.buildKey(module1, entity1, id1);
        String key2 = redisKeyUtil.buildKey(module2, entity2, id2);
        
        Set<String> union = stringRedisTemplate.opsForSet().union(key1, key2);
        log.info("Get union of {} and {} - count: {}", 
                key1, key2, union != null ? union.size() : 0);
        return union;
    }

    /**
     * Get difference between sets (members in first set but not in second)
     */
    public Set<String> getDifference(String module1, String entity1, String id1,
                                    String module2, String entity2, String id2) {
        String key1 = redisKeyUtil.buildKey(module1, entity1, id1);
        String key2 = redisKeyUtil.buildKey(module2, entity2, id2);
        
        Set<String> difference = stringRedisTemplate.opsForSet().difference(key1, key2);
        log.info("Get difference of {} and {} - count: {}", 
                key1, key2, difference != null ? difference.size() : 0);
        return difference;
    }

    /**
     * Store intersection result in destination set
     */
    public long storeIntersection(String destModule, String destEntity, String destId,
                                 String module1, String entity1, String id1,
                                 String module2, String entity2, String id2) {
        String destKey = redisKeyUtil.buildKey(destModule, destEntity, destId);
        String key1 = redisKeyUtil.buildKey(module1, entity1, id1);
        String key2 = redisKeyUtil.buildKey(module2, entity2, id2);
        
        long result = stringRedisTemplate.opsForSet().intersectAndStore(key1, key2, destKey);
        
        // Set TTL for destination set
        if (result > 0) {
            long ttl = ttlGenerator.generateTtl();
            stringRedisTemplate.expire(destKey, Duration.ofSeconds(ttl));
            log.info("Set TTL {} seconds for intersection result: {}", ttl, destKey);
        }
        
        log.info("Store intersection of {} and {} to {} - count: {}", key1, key2, destKey, result);
        return result;
    }
}
