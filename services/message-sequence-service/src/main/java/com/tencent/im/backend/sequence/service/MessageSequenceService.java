package com.tencent.im.backend.sequence.service;

import com.tencent.im.backend.common.core.constant.IMConstants;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 消息序列号服务
 * 保障消息有序性的核心组件
 */
@Service
public class MessageSequenceService {

    private static final Logger logger = LoggerFactory.getLogger(MessageSequenceService.class);

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 获取单聊消息序列号
     * @param fromUserId 发送者ID
     * @param toUserId 接收者ID
     * @return 序列号
     */
    public Long getC2CMessageSequence(String fromUserId, String toUserId) {
        // 为了保证两个用户之间的消息顺序，使用固定的key生成规则
        String key = generateC2CSequenceKey(fromUserId, toUserId);
        return getNextSequence(key);
    }

    /**
     * 获取群聊消息序列号
     * @param groupId 群组ID
     * @return 序列号
     */
    public Long getGroupMessageSequence(String groupId) {
        String key = IMConstants.MessageSequence.GROUP_SEQ_PREFIX + groupId;
        return getNextSequence(key);
    }

    /**
     * 重置单聊消息序列号
     * @param fromUserId 发送者ID
     * @param toUserId 接收者ID
     */
    public void resetC2CMessageSequence(String fromUserId, String toUserId) {
        String key = generateC2CSequenceKey(fromUserId, toUserId);
        resetSequence(key);
    }

    /**
     * 重置群聊消息序列号
     * @param groupId 群组ID
     */
    public void resetGroupMessageSequence(String groupId) {
        String key = IMConstants.MessageSequence.GROUP_SEQ_PREFIX + groupId;
        resetSequence(key);
    }

    /**
     * 获取当前单聊消息序列号（不自增）
     * @param fromUserId 发送者ID
     * @param toUserId 接收者ID
     * @return 当前序列号
     */
    public Long getCurrentC2CSequence(String fromUserId, String toUserId) {
        String key = generateC2CSequenceKey(fromUserId, toUserId);
        return getCurrentSequence(key);
    }

    /**
     * 获取当前群聊消息序列号（不自增）
     * @param groupId 群组ID
     * @return 当前序列号
     */
    public Long getCurrentGroupSequence(String groupId) {
        String key = IMConstants.MessageSequence.GROUP_SEQ_PREFIX + groupId;
        return getCurrentSequence(key);
    }

    /**
     * 验证消息序列号是否正确
     * @param key 序列号key
     * @param expectedSequence 期望的序列号
     * @return 是否正确
     */
    public boolean validateSequence(String key, Long expectedSequence) {
        Long currentSequence = getCurrentSequence(key);
        return expectedSequence.equals(currentSequence + 1);
    }

    /**
     * 设置序列号
     * @param key 序列号key
     * @param sequence 序列号值
     */
    public void setSequence(String key, Long sequence) {
        try {
            RAtomicLong atomicLong = redissonClient.getAtomicLong(key);
            atomicLong.set(sequence);
            atomicLong.expire(IMConstants.MessageSequence.SEQ_EXPIRE_TIME, TimeUnit.SECONDS);
            logger.debug("Sequence set: key={}, sequence={}", key, sequence);
        } catch (Exception e) {
            logger.error("Failed to set sequence: key={}, sequence={}", key, sequence, e);
            throw new RuntimeException("Failed to set sequence", e);
        }
    }

    /**
     * 获取下一个序列号
     */
    private Long getNextSequence(String key) {
        try {
            RAtomicLong atomicLong = redissonClient.getAtomicLong(key);
            long sequence = atomicLong.incrementAndGet();
            // 设置过期时间
            atomicLong.expire(IMConstants.MessageSequence.SEQ_EXPIRE_TIME, TimeUnit.SECONDS);
            logger.debug("Next sequence generated: key={}, sequence={}", key, sequence);
            return sequence;
        } catch (Exception e) {
            logger.error("Failed to get next sequence: key={}", key, e);
            throw new RuntimeException("Failed to get next sequence", e);
        }
    }

    /**
     * 获取当前序列号
     */
    private Long getCurrentSequence(String key) {
        try {
            RAtomicLong atomicLong = redissonClient.getAtomicLong(key);
            return atomicLong.get();
        } catch (Exception e) {
            logger.error("Failed to get current sequence: key={}", key, e);
            return 0L;
        }
    }

    /**
     * 重置序列号
     */
    private void resetSequence(String key) {
        try {
            RAtomicLong atomicLong = redissonClient.getAtomicLong(key);
            atomicLong.set(0);
            logger.info("Sequence reset: key={}", key);
        } catch (Exception e) {
            logger.error("Failed to reset sequence: key={}", key, e);
            throw new RuntimeException("Failed to reset sequence", e);
        }
    }

    /**
     * 生成单聊序列号key
     * 为了保证两个用户之间的消息顺序，使用用户ID的字典序排列
     */
    private String generateC2CSequenceKey(String fromUserId, String toUserId) {
        String user1 = fromUserId.compareTo(toUserId) < 0 ? fromUserId : toUserId;
        String user2 = fromUserId.compareTo(toUserId) < 0 ? toUserId : fromUserId;
        return IMConstants.MessageSequence.C2C_SEQ_PREFIX + user1 + ":" + user2;
    }
}
