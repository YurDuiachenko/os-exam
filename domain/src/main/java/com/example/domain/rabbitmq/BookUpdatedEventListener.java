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
public class BookUpdatedEventListener {
    private final ObjectMapper objectMapper;
    private final BookRepository bookRepository;

    @RabbitListener(queues = "bookUpdated")
    public void listen(String massage) {
        Book book;
        try {
            System.out.println(massage);
            book = objectMapper.readValue(massage, Book.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        var updatingBook = bookRepository.findById(book.getId())
                .orElse(new Book());
        if (book.getName() != null && !book.getName().isEmpty()) updatingBook.setName(book.getName());
        if (book.getAuthor() != null && !book.getAuthor().isEmpty()) updatingBook.setAuthor(book.getAuthor());
        if (book.getGenre() != null && !book.getGenre().isEmpty()) updatingBook.setGenre(book.getGenre());
        updatingBook.setYear(book.getYear());
        bookRepository.save(updatingBook);
    }
}