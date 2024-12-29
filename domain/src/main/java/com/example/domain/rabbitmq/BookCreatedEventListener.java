package com.example.domain.rabbitmq;

import com.example.domain.domain.Book;
import com.example.domain.repository.BookRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookCreatedEventListener {
    private final ObjectMapper objectMapper;
    private final BookRepository bookRepository;

    @RabbitListener(queues = "bookCreated")
    public void listen(String massage) {
        Book book;
        try {
            System.out.println(massage);
            book = objectMapper.readValue(massage, Book.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        bookRepository.save(book);
    }
}