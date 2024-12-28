package com.example.domain.config;

import com.example.domain.domain.Student;
import com.example.domain.repository.StudentRepository;
import io.grpc.Server;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GrpcServerStarter implements CommandLineRunner {
    private final Server grpcServer;
    private final StudentRepository studentRepository;

    @Override
    public void run(String... args) throws Exception {
        var student = new Student()
                .setFirstName("Yuri")
                .setLastName("Diachenko")
                .setAge(21)
                .setMajor("Computer Sciencs");
        studentRepository.save(student);

        grpcServer.start();
        grpcServer.awaitTermination();
    }
}