package com.tencent.im.backend.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.im.backend.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户数据访问层
 *
 * @author IM Backend Team
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户名查询用户（包含密码）
     *
     * @param username 用户名
     * @return 用户信息
     */
    @Select("SELECT * FROM users WHERE username = #{username} AND deleted = 0")
    User selectByUsernameWithPassword(@Param("username") String username);

    /**
     * 根据邮箱查询用户（包含密码）
     *
     * @param email 邮箱
     * @return 用户信息
     */
    @Select("SELECT * FROM users WHERE email = #{email} AND deleted = 0")
    User selectByEmailWithPassword(@Param("email") String email);

    /**
     * 更新用户最后登录信息
     *
     * @param userId 用户ID
     * @param loginIp 登录IP
     * @return 影响行数
     */
    @Select("UPDATE users SET last_login_at = NOW(), last_login_ip = #{loginIp} WHERE user_id = #{userId}")
    int updateLastLoginInfo(@Param("userId") String userId, @Param("loginIp") String loginIp);
}
