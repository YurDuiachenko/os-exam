package com.example.gateway.rest;

import book.BookOuterClass;
import book.BookServiceGrpc;
import com.example.gateway.redis.RedisOperations;
import com.example.gateway.rest.dto.BookDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
public class BookController {
    public static final String REDIS_KEY_BOOKS_ALL = "books::all";

    private final ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051)
            .usePlaintext()
            .build();
    private final BookServiceGrpc.BookServiceBlockingStub stub = BookServiceGrpc.newBlockingStub(channel);
    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;
    private final RedisOperations<BookDto> redisOperations;

    @GetMapping("/book/all")
    public List<BookDto> getAllBooks() {
        log.info("GET request for getting all books");
        List<BookDto> books;
        List<BookDto> cachedBooks = redisOperations.get(REDIS_KEY_BOOKS_ALL);

        if (cachedBooks.isEmpty()) {
            BookOuterClass.Empty request = BookOuterClass.Empty.newBuilder().build();
            books = stub.getAllBooks(request).getBooksList().stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());
        } else {
            return cachedBooks;
        }
        redisOperations.save(REDIS_KEY_BOOKS_ALL, books);

        return books;
    }

    @GetMapping("/book/{id}")
    public BookDto getBook(@PathVariable String id) {
        log.info("GET request for getting book with (id: {})", id);
        BookOuterClass.BookRequest request = BookOuterClass.BookRequest.newBuilder()
                .setId(id)
                .build();
        return toDto(stub.getBookById(request));
    }

    @PostMapping("/book/new")
    public void createBook(@RequestBody BookDto bookDto) {
        log.info("POST request for creating (book: {})", bookDto);
        try {
            String addBookJson = objectMapper.writeValueAsString(bookDto);
            rabbitTemplate.convertAndSend("bookCreated", addBookJson);
            redisOperations.delete(REDIS_KEY_BOOKS_ALL);
        } catch (JsonProcessingException e) {
            log.info("Error while writing JSON to string");
            throw new RuntimeException(e);
        }
    }

    @PutMapping("/book/update")
    public void updateBook(@RequestBody BookDto bookDto) {
        log.info("PUT request for updating (book: {})", bookDto);
        try {
            String updateBookJson = objectMapper.writeValueAsString(bookDto);
            rabbitTemplate.convertAndSend("bookUpdated", updateBookJson);
            redisOperations.delete(REDIS_KEY_BOOKS_ALL);
        } catch (JsonProcessingException e) {
            log.info("Error while writing JSON to string");
            throw new RuntimeException(e);
        }
    }

    @DeleteMapping("/book/{id}")
    public void deleteBook(@PathVariable String id) {
        log.info("DELETE request for deleting book with (id: {})", id);
        rabbitTemplate.convertAndSend("bookDeleted", id);
        redisOperations.delete(REDIS_KEY_BOOKS_ALL);
    }

    private BookDto toDto(BookOuterClass.Book book) {
        return new BookDto()
                .setId(book.getId())
                .setName(book.getName())
                .setAuthor(book.getAuthor())
                .setGenre(book.getGenre())
                .setYear(book.getYear());
    }
}
