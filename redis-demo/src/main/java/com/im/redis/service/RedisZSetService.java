package com.im.redis.service;

import com.im.redis.config.RedisConfig;
import com.im.redis.util.RedisKeyUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

/**
 * Redis Sorted Set (ZSet) Operations Service
 * Demonstrates ZSet operations for rankings and leaderboards
 */
@Slf4j
@Service
public class RedisZSetService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisKeyUtil redisKeyUtil;

    @Autowired
    private RedisConfig.TtlGenerator ttlGenerator;

    /**
     * Add member with score to sorted set
     */
    public boolean addMember(String module, String entity, String id, String member, double score) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        boolean result = stringRedisTemplate.opsForZSet().add(key, member, score);
        
        // Set TTL for new sorted set
        if (stringRedisTemplate.opsForZSet().zCard(key) == 1) {
            long ttl = ttlGenerator.generateTtl();
            stringRedisTemplate.expire(key, Duration.ofSeconds(ttl));
            log.info("Set TTL {} seconds for new sorted set key: {}", ttl, key);
        }
        
        log.info("Add member to sorted set - key: {}, member: {}, score: {}, added: {}", 
                key, member, score, result);
        return result;
    }

    /**
     * Add multiple members with scores
     */
    public long addMembers(String module, String entity, String id, Set<ZSetOperations.TypedTuple<String>> tuples) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        long result = stringRedisTemplate.opsForZSet().add(key, tuples);
        
        // Set TTL for new sorted set
        if (stringRedisTemplate.opsForZSet().zCard(key) == result) {
            long ttl = ttlGenerator.generateTtl();
            stringRedisTemplate.expire(key, Duration.ofSeconds(ttl));
            log.info("Set TTL {} seconds for new sorted set key: {}", ttl, key);
        }
        
        log.info("Add members to sorted set - key: {}, tuples count: {}, added count: {}", 
                key, tuples.size(), result);
        return result;
    }

    /**
     * Remove member from sorted set
     */
    public long removeMember(String module, String entity, String id, String member) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        long result = stringRedisTemplate.opsForZSet().remove(key, member);
        log.info("Remove member from sorted set - key: {}, member: {}, removed: {}", 
                key, member, result > 0);
        return result;
    }

    /**
     * Get member score
     */
    public Double getScore(String module, String entity, String id, String member) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        Double score = stringRedisTemplate.opsForZSet().score(key, member);
        log.info("Get member score - key: {}, member: {}, score: {}", key, member, score);
        return score;
    }

    /**
     * Get member rank (0-based, lowest score has rank 0)
     */
    public Long getRank(String module, String entity, String id, String member) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        Long rank = stringRedisTemplate.opsForZSet().rank(key, member);
        log.info("Get member rank - key: {}, member: {}, rank: {}", key, member, rank);
        return rank;
    }

    /**
     * Get member reverse rank (0-based, highest score has rank 0)
     */
    public Long getReverseRank(String module, String entity, String id, String member) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        Long rank = stringRedisTemplate.opsForZSet().reverseRank(key, member);
        log.info("Get member reverse rank - key: {}, member: {}, reverse rank: {}", key, member, rank);
        return rank;
    }

    /**
     * Increment member score
     */
    public double incrementScore(String module, String entity, String id, String member, double delta) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        double newScore = stringRedisTemplate.opsForZSet().incrementScore(key, member, delta);
        
        // Set TTL for new sorted set
        if (stringRedisTemplate.opsForZSet().zCard(key) == 1) {
            long ttl = ttlGenerator.generateTtl();
            stringRedisTemplate.expire(key, Duration.ofSeconds(ttl));
            log.info("Set TTL {} seconds for new sorted set key: {}", ttl, key);
        }
        
        log.info("Increment member score - key: {}, member: {}, delta: {}, new score: {}", 
                key, member, delta, newScore);
        return newScore;
    }

    /**
     * Get sorted set size
     */
    public long getSize(String module, String entity, String id) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        long size = stringRedisTemplate.opsForZSet().zCard(key);
        log.info("Get sorted set size - key: {}, size: {}", key, size);
        return size;
    }

    /**
     * Get count of members with scores between min and max
     */
    public long getCountByScore(String module, String entity, String id, double min, double max) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        long count = stringRedisTemplate.opsForZSet().count(key, min, max);
        log.info("Get count by score range - key: {}, min: {}, max: {}, count: {}", 
                key, min, max, count);
        return count;
    }

    /**
     * Get range by rank (ascending order)
     */
    public Set<String> getRangeByRank(String module, String entity, String id, long start, long end) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        Set<String> result = stringRedisTemplate.opsForZSet().range(key, start, end);
        log.info("Get range by rank - key: {}, start: {}, end: {}, count: {}", 
                key, start, end, result != null ? result.size() : 0);
        return result;
    }

    /**
     * Get range by rank with scores (ascending order)
     */
    public Set<ZSetOperations.TypedTuple<String>> getRangeWithScoresByRank(String module, String entity, String id, 
                                                                          long start, long end) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        Set<ZSetOperations.TypedTuple<String>> result = stringRedisTemplate.opsForZSet().rangeWithScores(key, start, end);
        log.info("Get range with scores by rank - key: {}, start: {}, end: {}, count: {}", 
                key, start, end, result != null ? result.size() : 0);
        return result;
    }

    /**
     * Get reverse range by rank (descending order)
     */
    public Set<String> getReverseRangeByRank(String module, String entity, String id, long start, long end) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        Set<String> result = stringRedisTemplate.opsForZSet().reverseRange(key, start, end);
        log.info("Get reverse range by rank - key: {}, start: {}, end: {}, count: {}", 
                key, start, end, result != null ? result.size() : 0);
        return result;
    }

    /**
     * Get reverse range with scores by rank (descending order)
     */
    public Set<ZSetOperations.TypedTuple<String>> getReverseRangeWithScoresByRank(String module, String entity, String id, 
                                                                                 long start, long end) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        Set<ZSetOperations.TypedTuple<String>> result = stringRedisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);
        log.info("Get reverse range with scores by rank - key: {}, start: {}, end: {}, count: {}", 
                key, start, end, result != null ? result.size() : 0);
        return result;
    }

    /**
     * Get range by score
     */
    public Set<String> getRangeByScore(String module, String entity, String id, double min, double max) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        Set<String> result = stringRedisTemplate.opsForZSet().rangeByScore(key, min, max);
        log.info("Get range by score - key: {}, min: {}, max: {}, count: {}", 
                key, min, max, result != null ? result.size() : 0);
        return result;
    }

    /**
     * Get range by score with limit
     */
    public Set<String> getRangeByScore(String module, String entity, String id, double min, double max, 
                                      long offset, long count) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        Set<String> result = stringRedisTemplate.opsForZSet().rangeByScore(key, min, max, offset, count);
        log.info("Get range by score with limit - key: {}, min: {}, max: {}, offset: {}, count: {}, result count: {}", 
                key, min, max, offset, count, result != null ? result.size() : 0);
        return result;
    }

    /**
     * Remove range by rank
     */
    public long removeRangeByRank(String module, String entity, String id, long start, long end) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        long result = stringRedisTemplate.opsForZSet().removeRange(key, start, end);
        log.info("Remove range by rank - key: {}, start: {}, end: {}, removed count: {}", 
                key, start, end, result);
        return result;
    }

    /**
     * Remove range by score
     */
    public long removeRangeByScore(String module, String entity, String id, double min, double max) {
        String key = redisKeyUtil.buildKey(module, entity, id);
        long result = stringRedisTemplate.opsForZSet().removeRangeByScore(key, min, max);
        log.info("Remove range by score - key: {}, min: {}, max: {}, removed count: {}", 
                key, min, max, result);
        return result;
    }

    /**
     * Get top N members (highest scores)
     */
    public Set<ZSetOperations.TypedTuple<String>> getTopN(String module, String entity, String id, int n) {
        return getReverseRangeWithScoresByRank(module, entity, id, 0, n - 1);
    }

    /**
     * Get bottom N members (lowest scores)
     */
    public Set<ZSetOperations.TypedTuple<String>> getBottomN(String module, String entity, String id, int n) {
        return getRangeWithScoresByRank(module, entity, id, 0, n - 1);
    }
}
