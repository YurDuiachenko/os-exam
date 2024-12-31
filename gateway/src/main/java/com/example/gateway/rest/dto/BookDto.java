package com.example.gateway.rest.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class BookDto {
    private String id;
    private String name;
    private String author;
    private String genre;
    private int year;
}
