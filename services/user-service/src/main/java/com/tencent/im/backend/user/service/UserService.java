package com.tencent.im.backend.user.service;

import com.tencent.im.backend.user.dto.SendVerificationCodeRequest;
import com.tencent.im.backend.user.dto.UserRegisterRequest;
import com.tencent.im.backend.user.dto.UserResponse;

/**
 * 用户服务接口
 *
 * @author IM Backend Team
 */
public interface UserService {

    /**
     * 发送邮箱验证码
     *
     * @param request 发送验证码请求
     * @return 是否发送成功
     */
    boolean sendVerificationCode(SendVerificationCodeRequest request);

    /**
     * 用户注册
     *
     * @param request 注册请求
     * @return 用户信息
     */
    UserResponse register(UserRegisterRequest request);

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    UserResponse getUserByUsername(String username);

    /**
     * 根据邮箱查询用户
     *
     * @param email 邮箱
     * @return 用户信息
     */
    UserResponse getUserByEmail(String email);

    /**
     * 根据用户ID查询用户
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    UserResponse getUserByUserId(String userId);
}
