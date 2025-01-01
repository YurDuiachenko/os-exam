package com.example.gateway.config;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {
    @Value("${redis.host}")
    private String redisHost;
    @Value("${redis.port}")
    private int redisPort;

    @Bean
    public RedisClient redisClient() {
        RedisURI redisUri = RedisURI.Builder.redis(redisHost, redisPort)
                .build();
        return RedisClient.create(redisUri);
    }
}
