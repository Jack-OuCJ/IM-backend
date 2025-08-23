package com.tencent.im.backend.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.im.backend.user.entity.EmailVerification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 邮箱验证数据访问层
 *
 * @author IM Backend Team
 */
@Mapper
public interface EmailVerificationMapper extends BaseMapper<EmailVerification> {

    /**
     * 查询指定邮箱在指定时间内的验证码数量
     *
     * @param email 邮箱
     * @param purpose 验证目的
     * @param sinceTime 开始时间
     * @return 验证码数量
     */
    @Select("SELECT COUNT(*) FROM email_verifications WHERE email = #{email} AND purpose = #{purpose} AND created_at >= #{sinceTime}")
    Long countRecentVerifications(@Param("email") String email, @Param("purpose") Integer purpose, @Param("sinceTime") String sinceTime);

    /**
     * 获取最新的有效验证码
     *
     * @param email 邮箱
     * @param verificationCode 验证码
     * @param purpose 验证目的
     * @return 验证码记录
     */
    @Select("SELECT * FROM email_verifications WHERE email = #{email} AND verification_code = #{verificationCode} AND purpose = #{purpose} AND verified = 0 AND expires_at > NOW() ORDER BY created_at DESC LIMIT 1")
    EmailVerification selectValidVerification(@Param("email") String email, @Param("verificationCode") String verificationCode, @Param("purpose") Integer purpose);

    /**
     * 批量标记验证码为已使用
     *
     * @param email 邮箱
     * @param purpose 验证目的
     * @return 影响行数
     */
    @Update("UPDATE email_verifications SET verified = 1 WHERE email = #{email} AND purpose = #{purpose} AND verified = 0")
    int markAllAsUsed(@Param("email") String email, @Param("purpose") Integer purpose);

    /**
     * 清理过期的验证码
     *
     * @return 清理的记录数
     */
    @Update("DELETE FROM email_verifications WHERE expires_at < NOW()")
    int cleanExpiredVerifications();
}
