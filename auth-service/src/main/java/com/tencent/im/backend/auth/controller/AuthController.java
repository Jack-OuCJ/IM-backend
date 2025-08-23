package com.tencent.im.backend.auth.controller;

import com.tencent.im.backend.auth.model.LoginRequest;
import com.tencent.im.backend.auth.model.LoginResponse;
import com.tencent.im.backend.auth.model.UserSigResponse;
import com.tencent.im.backend.auth.service.AuthService;
import com.tencent.im.backend.common.core.model.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证管理", description = "用户认证相关API")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public BaseResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        // 这里简化处理，实际项目中需要验证用户名密码
        String userId = request.getUsername(); // 简化：用户名作为用户ID
        
        // 生成JWT Token
        String jwtToken = authService.generateJwtToken(userId, request.getUsername());
        
        // 生成UserSig
        String userSig = authService.generateUserSig(userId);
        
        LoginResponse response = new LoginResponse();
        response.setUserId(userId);
        response.setUsername(request.getUsername());
        response.setJwtToken(jwtToken);
        response.setUserSig(userSig);
        
        return BaseResponse.success(response);
    }

    @Operation(summary = "获取UserSig")
    @GetMapping("/usersig/{userId}")
    public BaseResponse<UserSigResponse> getUserSig(@PathVariable String userId,
                                                   @RequestHeader("Authorization") String authorization) {
        // 验证JWT Token
        String token = authorization.replace("Bearer ", "");
        String tokenUserId = authService.getUserIdFromToken(token);
        
        if (!userId.equals(tokenUserId)) {
            return BaseResponse.error(403, "Forbidden");
        }
        
        // 生成UserSig
        String userSig = authService.generateUserSig(userId);
        
        UserSigResponse response = new UserSigResponse();
        response.setUserId(userId);
        response.setUserSig(userSig);
        response.setExpireTime(System.currentTimeMillis() + 604800 * 1000); // 7天
        
        return BaseResponse.success(response);
    }

    @Operation(summary = "验证Token")
    @GetMapping("/verify")
    public BaseResponse<String> verifyToken(@RequestHeader("Authorization") String authorization) {
        try {
            String token = authorization.replace("Bearer ", "");
            String userId = authService.getUserIdFromToken(token);
            
            if (userId != null) {
                return BaseResponse.success("Token valid", userId);
            } else {
                return BaseResponse.error(401, "Invalid token");
            }
        } catch (Exception e) {
            return BaseResponse.error(401, "Invalid token");
        }
    }

    @Operation(summary = "刷新Token")
    @PostMapping("/refresh")
    public BaseResponse<LoginResponse> refreshToken(@RequestHeader("Authorization") String authorization) {
        try {
            String token = authorization.replace("Bearer ", "");
            String userId = authService.getUserIdFromToken(token);
            
            if (userId != null) {
                // 生成新的JWT Token
                String newJwtToken = authService.generateJwtToken(userId, userId);
                
                // 生成新的UserSig
                String newUserSig = authService.generateUserSig(userId);
                
                LoginResponse response = new LoginResponse();
                response.setUserId(userId);
                response.setUsername(userId);
                response.setJwtToken(newJwtToken);
                response.setUserSig(newUserSig);
                
                return BaseResponse.success(response);
            } else {
                return BaseResponse.error(401, "Invalid token");
            }
        } catch (Exception e) {
            return BaseResponse.error(401, "Invalid token");
        }
    }
}
