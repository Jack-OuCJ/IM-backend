package com.tencent.im.backend.auth.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

/**
 * 认证服务
 * 负责JWT Token生成验证和腾讯云IM UserSig生成
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Value("${auth.jwt.secret:im-backend-secret}")
    private String jwtSecret;

    @Value("${auth.jwt.expiration:3600}")
    private Long jwtExpiration;

    @Value("${tencent.im.sdk-app-id}")
    private String sdkAppId;

    @Value("${tencent.im.secret-key}")
    private String secretKey;

    @Value("${tencent.im.usersig.expiration:604800}")
    private Long userSigExpiration; // 7天

    /**
     * 生成JWT Token
     */
    public String generateJwtToken(String userId, String username) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
            
            return JWT.create()
                    .withSubject(userId)
                    .withClaim("username", username)
                    .withClaim("sdkAppId", sdkAppId)
                    .withIssuedAt(new Date())
                    .withExpiresAt(Date.from(Instant.now().plus(jwtExpiration, ChronoUnit.SECONDS)))
                    .sign(algorithm);
        } catch (Exception e) {
            logger.error("Failed to generate JWT token for user: {}", userId, e);
            throw new RuntimeException("Failed to generate JWT token", e);
        }
    }

    /**
     * 验证JWT Token
     */
    public DecodedJWT verifyJwtToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
            JWTVerifier verifier = JWT.require(algorithm).build();
            return verifier.verify(token);
        } catch (JWTVerificationException e) {
            logger.error("JWT token verification failed: {}", token, e);
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

    /**
     * 生成腾讯云IM UserSig
     */
    public String generateUserSig(String userId) {
        try {
            // 生成时间戳
            long currentTime = System.currentTimeMillis() / 1000;
            long expireTime = currentTime + userSigExpiration;

            // 构建签名字符串
            String signatureString = String.format(
                    "TLS.identifier:%s\n" +
                    "TLS.sdkappid:%s\n" +
                    "TLS.time:%d\n" +
                    "TLS.expire:%d\n",
                    userId, sdkAppId, currentTime, userSigExpiration
            );

            // 使用HMAC-SHA256签名
            byte[] signature = hmacSha256(signatureString.getBytes(), secretKey.getBytes());
            String base64Signature = Base64.getEncoder().encodeToString(signature);

            // 构建UserSig JSON
            String userSigJson = String.format(
                    "{" +
                    "\"TLS.ver\":\"2.0\"," +
                    "\"TLS.identifier\":\"%s\"," +
                    "\"TLS.sdkappid\":%s," +
                    "\"TLS.expire\":%d," +
                    "\"TLS.time\":%d," +
                    "\"TLS.sig\":\"%s\"" +
                    "}",
                    userId, sdkAppId, userSigExpiration, currentTime, base64Signature
            );

            // 压缩并Base64编码
            byte[] compressedData = compress(userSigJson.getBytes());
            String userSig = Base64.getEncoder().encodeToString(compressedData);

            logger.debug("UserSig generated for user: {}", userId);
            return userSig;
        } catch (Exception e) {
            logger.error("Failed to generate UserSig for user: {}", userId, e);
            throw new RuntimeException("Failed to generate UserSig", e);
        }
    }

    /**
     * 验证UserSig
     */
    public boolean verifyUserSig(String userId, String userSig) {
        try {
            // 解码并解压缩
            byte[] compressedData = Base64.getDecoder().decode(userSig);
            String userSigJson = new String(decompress(compressedData));

            // 这里可以解析JSON并验证签名
            // 简单起见，重新生成UserSig进行比较
            String expectedUserSig = generateUserSig(userId);
            return userSig.equals(expectedUserSig);
        } catch (Exception e) {
            logger.error("Failed to verify UserSig for user: {}", userId, e);
            return false;
        }
    }

    /**
     * 从JWT Token中提取用户ID
     */
    public String getUserIdFromToken(String token) {
        try {
            DecodedJWT decodedJWT = verifyJwtToken(token);
            return decodedJWT.getSubject();
        } catch (Exception e) {
            logger.error("Failed to extract userId from token: {}", token, e);
            return null;
        }
    }

    /**
     * 检查JWT Token是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            DecodedJWT decodedJWT = verifyJwtToken(token);
            return decodedJWT.getExpiresAt().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * HMAC-SHA256签名
     */
    private byte[] hmacSha256(byte[] data, byte[] key) throws Exception {
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(key, "HmacSHA256");
        mac.init(secretKeySpec);
        return mac.doFinal(data);
    }

    /**
     * 简单的数据压缩（实际项目中建议使用zlib）
     */
    private byte[] compress(byte[] data) {
        // 简单起见，这里不做压缩，直接返回原数据
        // 实际项目中应该使用zlib压缩
        return data;
    }

    /**
     * 简单的数据解压缩
     */
    private byte[] decompress(byte[] data) {
        // 简单起见，这里不做解压缩，直接返回原数据
        return data;
    }
}
