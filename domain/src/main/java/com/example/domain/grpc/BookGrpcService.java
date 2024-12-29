package com.example.domain.grpc;

import com.example.domain.domain.Book;
import com.example.domain.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import book.BookServiceGrpc;
import book.BookOuterClass;

@Component
@RequiredArgsConstructor
public class BookGrpcService extends BookServiceGrpc.BookServiceImplBase {
    private final BookRepository bookRepository;

    @Override
    public void getAllBooks(BookOuterClass.Empty request, io.grpc.stub.StreamObserver<BookOuterClass.BookList> responseObserver) {
        var books = bookRepository.findAll().stream()
                .map(this::map)
                .toList();

        BookOuterClass.BookList bookList = BookOuterClass.BookList.newBuilder()
                .addAllBooks(books)
                .build();

        responseObserver.onNext(bookList);
        responseObserver.onCompleted();
    }

    @Override
    public void getBookById(BookOuterClass.BookRequest request, io.grpc.stub.StreamObserver<BookOuterClass.Book> responseObserver) {
        var book = map(bookRepository.findById(request.getId()).orElse(new Book()));

        responseObserver.onNext(book);
        responseObserver.onCompleted();
    }

    private BookOuterClass.Book map(Book book) {
        return BookOuterClass.Book.newBuilder()
                .setId(book.getId() != null ? book.getId() : "")
                .setName(book.getName() != null ? book.getName() : "")
                .setAuthor(book.getAuthor() != null ? book.getAuthor() : "")
                .setGenre(book.getGenre() != null ? book.getGenre() : "")
                .setYear(book.getYear())
                .build();
    }
}