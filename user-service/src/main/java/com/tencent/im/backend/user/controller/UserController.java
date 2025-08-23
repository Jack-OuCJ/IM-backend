package com.tencent.im.backend.user.controller;

import com.tencent.im.backend.user.dto.SendVerificationCodeRequest;
import com.tencent.im.backend.user.dto.UserRegisterRequest;
import com.tencent.im.backend.user.dto.UserResponse;
import com.tencent.im.backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户控制器
 *
 * @author IM Backend Team
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Validated
@Tag(name = "用户管理", description = "用户注册、登录等相关接口")
public class UserController {

    private final UserService userService;

    @PostMapping("/send-verification-code")
    @Operation(summary = "发送邮箱验证码", description = "向指定邮箱发送验证码")
    public ResponseEntity<Map<String, Object>> sendVerificationCode(
            @Valid @RequestBody SendVerificationCodeRequest request) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean success = userService.sendVerificationCode(request);
            if (success) {
                response.put("code", 200);
                response.put("message", "验证码发送成功");
                response.put("data", null);
            } else {
                response.put("code", 500);
                response.put("message", "验证码发送失败");
                response.put("data", null);
            }
        } catch (Exception e) {
            log.error("发送验证码失败：{}", e.getMessage(), e);
            response.put("code", 400);
            response.put("message", e.getMessage());
            response.put("data", null);
        }
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "通过邮箱验证码进行用户注册")
    public ResponseEntity<Map<String, Object>> register(
            @Valid @RequestBody UserRegisterRequest request) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            UserResponse userResponse = userService.register(request);
            response.put("code", 200);
            response.put("message", "注册成功");
            response.put("data", userResponse);
        } catch (Exception e) {
            log.error("用户注册失败：{}", e.getMessage(), e);
            response.put("code", 400);
            response.put("message", e.getMessage());
            response.put("data", null);
        }
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/username/{username}")
    @Operation(summary = "根据用户名查询用户", description = "通过用户名获取用户信息")
    public ResponseEntity<Map<String, Object>> getUserByUsername(
            @Parameter(description = "用户名") @PathVariable String username) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            UserResponse userResponse = userService.getUserByUsername(username);
            if (userResponse != null) {
                response.put("code", 200);
                response.put("message", "查询成功");
                response.put("data", userResponse);
            } else {
                response.put("code", 404);
                response.put("message", "用户不存在");
                response.put("data", null);
            }
        } catch (Exception e) {
            log.error("查询用户失败：{}", e.getMessage(), e);
            response.put("code", 500);
            response.put("message", "查询失败");
            response.put("data", null);
        }
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "根据邮箱查询用户", description = "通过邮箱获取用户信息")
    public ResponseEntity<Map<String, Object>> getUserByEmail(
            @Parameter(description = "邮箱") @PathVariable String email) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            UserResponse userResponse = userService.getUserByEmail(email);
            if (userResponse != null) {
                response.put("code", 200);
                response.put("message", "查询成功");
                response.put("data", userResponse);
            } else {
                response.put("code", 404);
                response.put("message", "用户不存在");
                response.put("data", null);
            }
        } catch (Exception e) {
            log.error("查询用户失败：{}", e.getMessage(), e);
            response.put("code", 500);
            response.put("message", "查询失败");
            response.put("data", null);
        }
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "根据用户ID查询用户", description = "通过用户ID获取用户信息")
    public ResponseEntity<Map<String, Object>> getUserByUserId(
            @Parameter(description = "用户ID") @PathVariable String userId) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            UserResponse userResponse = userService.getUserByUserId(userId);
            if (userResponse != null) {
                response.put("code", 200);
                response.put("message", "查询成功");
                response.put("data", userResponse);
            } else {
                response.put("code", 404);
                response.put("message", "用户不存在");
                response.put("data", null);
            }
        } catch (Exception e) {
            log.error("查询用户失败：{}", e.getMessage(), e);
            response.put("code", 500);
            response.put("message", "查询失败");
            response.put("data", null);
        }
        
        return ResponseEntity.ok(response);
    }
}
