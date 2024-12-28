package com.example.domain.config;

import com.example.domain.grpc.StudentGrpcService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcServerConfig {
    @Bean
    public Server grpcServer(StudentGrpcService studentGrpcService) {
        return ServerBuilder.forPort(50051)
                .addService(studentGrpcService)
                .build();
    }
}