package com.tencent.im.backend.user.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tencent.im.backend.user.dto.SendVerificationCodeRequest;
import com.tencent.im.backend.user.dto.UserRegisterRequest;
import com.tencent.im.backend.user.dto.UserResponse;
import com.tencent.im.backend.user.entity.EmailVerification;
import com.tencent.im.backend.user.entity.User;
import com.tencent.im.backend.user.mapper.EmailVerificationMapper;
import com.tencent.im.backend.user.mapper.UserMapper;
import com.tencent.im.backend.user.service.EmailService;
import com.tencent.im.backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 用户服务实现类
 *
 * @author IM Backend Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final EmailVerificationMapper emailVerificationMapper;
    private final EmailService emailService;

    @Override
    public boolean sendVerificationCode(SendVerificationCodeRequest request) {
        // 检查邮箱是否已注册（如果是注册验证码）
        if (request.getPurpose() == EmailVerification.Purpose.REGISTER.getCode()) {
            User existingUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, request.getEmail())
            );
            if (existingUser != null) {
                throw new RuntimeException("该邮箱已被注册");
            }
        }

        // 检查是否在短时间内重复发送
        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
        Long recentCount = emailVerificationMapper.selectCount(
            new LambdaQueryWrapper<EmailVerification>()
                .eq(EmailVerification::getEmail, request.getEmail())
                .eq(EmailVerification::getPurpose, request.getPurpose())
                .ge(EmailVerification::getCreatedAt, oneMinuteAgo)
        );
        
        if (recentCount > 0) {
            throw new RuntimeException("验证码发送过于频繁，请稍后再试");
        }

        // 生成6位数字验证码
        String verificationCode = RandomUtil.randomNumbers(6);
        
        // 保存验证码到数据库
        EmailVerification emailVerification = new EmailVerification();
        emailVerification.setEmail(request.getEmail());
        emailVerification.setVerificationCode(verificationCode);
        emailVerification.setPurpose(request.getPurpose());
        emailVerification.setExpiresAt(LocalDateTime.now().plusMinutes(5)); // 5分钟过期
        emailVerification.setVerified(0);
        emailVerification.setCreatedAt(LocalDateTime.now());
        emailVerification.setUpdatedAt(LocalDateTime.now());
        
        int saved = emailVerificationMapper.insert(emailVerification);
        if (saved <= 0) {
            throw new RuntimeException("验证码保存失败");
        }

        // 发送邮件
        return emailService.sendVerificationCode(request.getEmail(), verificationCode);
    }

    @Override
    @Transactional
    public UserResponse register(UserRegisterRequest request) {
        // 验证密码一致性
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("密码与确认密码不一致");
        }

        // 检查用户名是否已存在
        User existingUserByUsername = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername())
        );
        if (existingUserByUsername != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 检查邮箱是否已注册
        User existingUserByEmail = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getEmail, request.getEmail())
        );
        if (existingUserByEmail != null) {
            throw new RuntimeException("该邮箱已被注册");
        }

        // 验证邮箱验证码
        EmailVerification emailVerification = emailVerificationMapper.selectOne(
            new LambdaQueryWrapper<EmailVerification>()
                .eq(EmailVerification::getEmail, request.getEmail())
                .eq(EmailVerification::getVerificationCode, request.getVerificationCode())
                .eq(EmailVerification::getPurpose, EmailVerification.Purpose.REGISTER.getCode())
                .eq(EmailVerification::getVerified, 0)
                .gt(EmailVerification::getExpiresAt, LocalDateTime.now())
                .orderByDesc(EmailVerification::getCreatedAt)
                .last("LIMIT 1")
        );

        if (emailVerification == null) {
            throw new RuntimeException("验证码无效或已过期");
        }

        // 创建用户
        User user = new User();
        user.setUserId(IdUtil.getSnowflakeNextIdStr()); // 生成雪花ID
        user.setUsername(request.getUsername());
        user.setPassword(BCrypt.hashpw(request.getPassword())); // BCrypt加密密码
        user.setEmail(request.getEmail());
        user.setEmailVerified(1); // 邮箱已验证
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setStatus(1); // 正常状态
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        int inserted = userMapper.insert(user);
        if (inserted <= 0) {
            throw new RuntimeException("用户注册失败");
        }

        // 标记验证码为已使用
        emailVerification.setVerified(1);
        emailVerification.setUpdatedAt(LocalDateTime.now());
        emailVerificationMapper.updateById(emailVerification);

        // 返回用户信息
        return convertToUserResponse(user);
    }

    @Override
    public UserResponse getUserByUsername(String username) {
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );
        return user != null ? convertToUserResponse(user) : null;
    }

    @Override
    public UserResponse getUserByEmail(String email) {
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getEmail, email)
        );
        return user != null ? convertToUserResponse(user) : null;
    }

    @Override
    public UserResponse getUserByUserId(String userId) {
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getUserId, userId)
        );
        return user != null ? convertToUserResponse(user) : null;
    }

    /**
     * 转换为用户响应DTO
     */
    private UserResponse convertToUserResponse(User user) {
        UserResponse response = new UserResponse();
        BeanUtils.copyProperties(user, response);
        response.setEmailVerified(user.getEmailVerified() == 1);
        return response;
    }
}
