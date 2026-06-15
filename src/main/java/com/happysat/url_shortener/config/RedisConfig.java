package com.happysat.url_shortener.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class RedisConfig {
    // Spring Boot auto-configures RedisCacheManager from:
    // - spring.data.redis.* (connection)
    // - spring.cache.redis.time-to-live (TTL)
}
