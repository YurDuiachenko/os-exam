package com.example.domain.config;

import com.example.domain.grpc.BookGrpcService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcServerConfig {
    @Bean
    public Server grpcServer(BookGrpcService bookGrpcService) {
        return ServerBuilder.forPort(50051)
                .addService(bookGrpcService)
                .build();
    }
}