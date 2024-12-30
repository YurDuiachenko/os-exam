package com.example.domain.config;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RabbitMQConfig {
    @Value("${rabbitmq.queues.student-created}")
    private String studentCreatedQueueName;
    @Value("${rabbitmq.queues.student-updated}")
    private String studentUpdatedQueueName;
    @Value("${rabbitmq.queues.student-deleted}")
    private String studentDeletedQueueName;

    @Bean
    public Queue studentCreatedQueue() {
        return new Queue(studentCreatedQueueName, false);
    }

    @Bean
    public Queue studentUpdatedQueue() {
        return new Queue(studentUpdatedQueueName, false);
    }

    @Bean
    public Queue studentDeletedQueue() {
        return new Queue(studentDeletedQueueName, false);
    }
}
