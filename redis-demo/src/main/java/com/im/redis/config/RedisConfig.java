package com.im.redis.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.Random;

@Configuration
public class RedisConfig {

    @Value("${redis.key.ttl.default:600}")
    private int defaultTtl;
    
    @Value("${redis.key.ttl.jitter-range:120}")
    private int jitterRange;

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use Jackson2JsonRedisSerializer for value serialization
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(mapper);

        // String serializer for keys
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        // Set key serialization
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);

        // Set value serialization
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }

    /**
     * Generate TTL with jitter to prevent cache avalanche
     */
    @Bean
    public TtlGenerator ttlGenerator() {
        return new TtlGenerator(defaultTtl, jitterRange);
    }

    public static class TtlGenerator {
        private final int baseTtl;
        private final int jitterRange;
        private final Random random;

        public TtlGenerator(int baseTtl, int jitterRange) {
            this.baseTtl = baseTtl;
            this.jitterRange = jitterRange;
            this.random = new Random();
        }

        /**
         * Generate TTL with random jitter
         * @return TTL in seconds with jitter applied
         */
        public long generateTtl() {
            int jitter = random.nextInt(jitterRange * 2) - jitterRange; // ±jitterRange
            return baseTtl + jitter;
        }

        /**
         * Generate custom TTL with jitter
         * @param customTtl base TTL value
         * @return TTL with jitter applied
         */
        public long generateTtl(int customTtl) {
            int jitter = random.nextInt(jitterRange * 2) - jitterRange;
            return Math.max(60, customTtl + jitter); // Minimum 60 seconds
        }
    }
}
