package com.nxq.perform.stackoverflow.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature; // Import thêm
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule; // Import thêm
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    @Bean(name = "localSearchCache")
    public Cache<String, Object> caffeineCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(2000)
                .recordStats()
                .build();
    }

    // Bean Cache byte[] cho giải pháp tối ưu JSON (Optional - dùng cho bước tối ưu sau)
    @Bean(name = "localJsonCache")
    public Cache<String, byte[]> caffeineJsonCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(2000)
                .recordStats()
                .build();
    }

    /**
     * KEY CHỐT HẠ: Template này chuyên trị byte[].
     * Nó giúp Redis trả về data thô, không tốn CPU để parse ra Object.
     */
    @Bean
    public RedisTemplate<String, byte[]> byteRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, byte[]> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key vẫn là String cho dễ đọc
        template.setKeySerializer(new StringRedisSerializer());

        // Value là Byte Array (Copy thẳng từ Redis ra RAM, CPU không cần làm việc)
        template.setValueSerializer(RedisSerializer.byteArray());

        return template;
    }

    /**
     * ObjectMapper sạch để serialize DTO -> JSON Bytes thủ công
     */
    @Bean
    public ObjectMapper simpleObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}