package com.example.domain.domain;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "books")
@Accessors(chain = true)
public class Book {
    @Id
    private String id;
    private String name;
    private String author;
    private String genre;
    private int year;
}