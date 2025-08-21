package com.tencent.im.backend.auth.service;

import com.tencent.im.backend.auth.config.IMProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

public class AuthServiceTest {

    private AuthService authService;
    private IMProperties imProperties;
    
    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 创建 IMProperties 实例并设置测试数据
        imProperties = new IMProperties();
        imProperties.setSdkAppId(1400000000L);
        imProperties.getUserSig().setExpireSeconds(86400L);
        imProperties.getPrivateKey().setRef("file:///tmp/test_private_key.pem");
        
        // 创建 AuthService 实例
        authService = new AuthService();
        
        // 注入 IMProperties
        ReflectionTestUtils.setField(authService, "imProperties", imProperties);
    }

    @Test
    public void testGenerateUserSig() {
        // 测试用户ID
        String userId = "dev01";
        
        System.out.println("=== 测试 generateUserSig 函数 ===");
        System.out.println("输入用户ID: " + userId);
        
        try {
            // 调用 generateUserSig 函数
            String userSig = authService.generateUserSig(userId);
            
            System.out.println("生成的 UserSig: " + userSig);
            System.out.println("UserSig 长度: " + userSig.length());
            
            // 验证结果
            assertNotNull(userSig, "UserSig 不应该为空");
            assertFalse(userSig.isEmpty(), "UserSig 不应该为空字符串");
            assertTrue(userSig.length() > 0, "UserSig 应该有内容");
            
            System.out.println("✓ 测试通过：成功生成 UserSig");
            
        } catch (Exception e) {
            System.err.println("✗ 测试失败：" + e.getMessage());
            e.printStackTrace();
            fail("生成 UserSig 时发生异常: " + e.getMessage());
        }
    }

    @Test
    public void testGenerateUserSigWithNullUserId() {
        System.out.println("=== 测试 generateUserSig 函数（空用户ID）===");
        
        // 测试空用户ID的情况
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            authService.generateUserSig(null);
        });
        
        System.out.println("预期异常信息: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("userId cannot be null or empty"));
        
        System.out.println("✓ 测试通过：正确处理了空用户ID");
    }

    @Test
    public void testGenerateUserSigWithEmptyUserId() {
        System.out.println("=== 测试 generateUserSig 函数（空字符串用户ID）===");
        
        // 测试空字符串用户ID的情况
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            authService.generateUserSig("");
        });
        
        System.out.println("预期异常信息: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("userId cannot be null or empty"));
        
        System.out.println("✓ 测试通过：正确处理了空字符串用户ID");
    }
}
