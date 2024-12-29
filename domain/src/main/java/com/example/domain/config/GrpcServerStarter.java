package com.example.domain.config;

import com.example.domain.domain.Book;
import com.example.domain.repository.BookRepository;
import io.grpc.Server;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GrpcServerStarter implements CommandLineRunner {
    private final Server grpcServer;
    private final BookRepository bookRepository;

    @Override
    public void run(String... args) throws Exception {
        var student = new Book()
                .setName("Black man")
                .setAuthor("Esenin")
                .setGenre("Poem")
                .setYear(1921);
        bookRepository.save(student);

        grpcServer.start();
        grpcServer.awaitTermination();
    }
}