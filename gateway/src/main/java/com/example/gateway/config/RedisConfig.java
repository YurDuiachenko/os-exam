package com.example.gateway.config;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {
    @Bean
    public RedisClient redisClient() {
        RedisURI redisUri = RedisURI.Builder.redis("localhost", 6379)
                .build();
        return RedisClient.create(redisUri);
    }
}
