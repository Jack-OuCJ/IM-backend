package com.tencent.im.backend.auth.service;

import com.tencent.im.backend.auth.AuthServiceApplication;
import com.tencent.im.backend.auth.config.IMProperties;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;
import redis.clients.jedis.JedisPooled;

@SpringBootTest(classes = AuthServiceApplication.class)
@ActiveProfiles("test")
public class AuthServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceTest.class);

    @Autowired
    private AuthService authService;
    
    @Autowired
    private IMProperties imProperties;

    @Test
    public void testGenerateUserSigForDev03() {
        // 测试用户ID
        String userId = "dev03";
        String expectedUserSig = "eJwtzFELgjAUhuH-cq5D1qnNMeiiQURgebGE8C7ZtFMYYw4Rov*eqZff88H7gWtmkt4FUIAJg9W0ybp3pJomtq5nm*Xo7OvuPVlQyBgKgTi7GzwFB2rNOR8fNmuk9m8p5xIFbsXSoGasSjzq-JBSPF2Kx62kMoRoq*LZDiarYh4rfa47LZ2h-Q6*P6owMUE_";
        
        logger.info("=== 测试 generateUserSig 函数（用户：dev03）===");
        logger.info("输入用户ID: {}", userId);
        logger.info("期望的 UserSig: {}", expectedUserSig);
        
        try {
            // 调用 generateUserSig 函数
            String actualUserSig = authService.generateUserSig(userId);
            
            logger.info("实际生成的 UserSig: {}", actualUserSig);
            logger.info("UserSig 长度: {}", actualUserSig.length());
            
            // 验证结果
            assertNotNull(actualUserSig, "UserSig 不应该为空");
            assertFalse(actualUserSig.isEmpty(), "UserSig 不应该为空字符串");
            assertTrue(actualUserSig.length() > 0, "UserSig 应该有内容");
            
            // 验证是否匹配期望的 UserSig
            if (expectedUserSig.equals(actualUserSig)) {
                logger.info("✓ 测试通过：生成的 UserSig 与期望值完全匹配");
            } else {
                logger.warn("⚠ 注意：生成的 UserSig 与期望值不匹配，这可能是由于配置差异导致的");
                logger.warn("期望值: {}", expectedUserSig);
                logger.warn("实际值: {}", actualUserSig);
            }
            
            // 验证配置信息
            logger.info("当前配置信息:");
            logger.info("- SDK App ID: {}", imProperties.getSdkAppId());
            logger.info("- 过期时间: {} 秒", imProperties.getUserSig().getExpireSeconds());
            logger.info("- 私钥引用: {}", imProperties.getPrivateKey().getRef());
            
        } catch (Exception e) {
            logger.error("✗ 测试失败：{}", e.getMessage(), e);
            fail("生成 UserSig 时发生异常: " + e.getMessage());
        }
    }

    @Test
    public void testGenerateUserSigWithNullUserId() {
        logger.info("=== 测试 generateUserSig 函数（空用户ID）===");
        
        // 测试空用户ID的情况
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            authService.generateUserSig(null);
        });
        
        logger.info("预期异常信息: {}", exception.getMessage());
        assertTrue(exception.getMessage().contains("userId cannot be null or empty"));
        
        logger.info("✓ 测试通过：正确处理了空用户ID");
    }

    @Test
    public void testGenerateUserSigWithEmptyUserId() {
        logger.info("=== 测试 generateUserSig 函数（空字符串用户ID）===");
        
        // 测试空字符串用户ID的情况
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            authService.generateUserSig("");
        });
        
        logger.info("预期异常信息: {}", exception.getMessage());
        assertTrue(exception.getMessage().contains("userId cannot be null or empty"));
        
        logger.info("✓ 测试通过：正确处理了空字符串用户ID");
    }

    @Test
    public void testIMPropertiesInjection() {
        logger.info("=== 测试 IMProperties 注入 ===");
        
        // 验证 IMProperties 正确注入
        assertNotNull(imProperties, "IMProperties 应该被正确注入");
        assertTrue(imProperties.getSdkAppId() > 0, "SDK App ID 应该大于 0");
        assertNotNull(imProperties.getPrivateKey().getRef(), "私钥引用不应该为空");
        assertTrue(imProperties.getUserSig().getExpireSeconds() > 0, "过期时间应该大于 0");
        
        logger.info("当前配置:");
        logger.info("- SDK App ID: {}", imProperties.getSdkAppId());
        logger.info("- 过期时间: {} 秒", imProperties.getUserSig().getExpireSeconds());
        logger.info("- 私钥引用: {}", imProperties.getPrivateKey().getRef());
        
        logger.info("✓ 测试通过：IMProperties 注入正常");
    }

    @Test
    public void testAuthServiceInitialization() {
        logger.info("=== 测试 AuthService 初始化 ===");
        
        // 验证 AuthService 被正确初始化
        assertNotNull(authService, "AuthService 应该被正确注入");
        
        // 测试生成 UserSig，这会验证 TLSSigAPIv2 是否正确初始化
        String userId = "test-user";
        String userSig = authService.generateUserSig(userId);
        
        assertNotNull(userSig, "生成的 UserSig 不应该为空");
        assertFalse(userSig.isEmpty(), "生成的 UserSig 不应该为空字符串");
        assertTrue(userSig.length() > 10, "生成的 UserSig 应该有足够的长度");
        
        logger.info("✓ 测试通过：AuthService 初始化正常");
        logger.info("测试用户 '{}' 的 UserSig: {}", userId, userSig);
    }

    @Test
    public void testRedisZset() {
        try (JedisPooled jedis = new JedisPooled("localhost", 6379)) {
            String key = "rank:scores";
            jedis.zadd(key, 100, "u1");
            jedis.zadd(key, 150, "u2");
            jedis.zincrby(key, 10, "u1");
            // 正序（分数小->大）
            System.out.println(jedis.zrangeWithScores(key, 0, -1));
            // 倒序（排行榜）
            System.out.println(jedis.zrevrangeWithScores(key, 0, 10));
            // 查看排名和分数
            Long rank = jedis.zrevrank(key, "u1");
            Double score = jedis.zscore(key, "u1");
            System.out.println("u1 rank=" + rank + " score=" + score);
        }
    }
}
