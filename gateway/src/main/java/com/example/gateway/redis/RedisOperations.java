package com.example.gateway.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.RedisClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Component
@Slf4j
public class RedisOperations<T> {
    private final ObjectMapper objectMapper;
    private final RedisClient redisClient;

    public RedisOperations(ObjectMapper objectMapper, RedisClient redisClient) {
        this.objectMapper = objectMapper;
        this.redisClient = redisClient;
    }

    public List<T> get(String key) {
        String cache = redisClient.connect().sync().get(key);

        if (cache != null) {
            try {
                return objectMapper.readValue(cache, new TypeReference<List<T>>() {});
            } catch (JsonProcessingException e) {
                log.error("Error parsing from cache",  e);
            }
        }
        return List.of();
    }

    public void save(String key, List<T> toCache) {
        try {
            redisClient.connect().sync().set(key, objectMapper.writeValueAsString(toCache));
        } catch (JsonProcessingException e) {
            log.error("Error writing to cache", e);
        }
    }

    public void delete(String key) {
        redisClient.connect().sync().del(key);
    }
}
