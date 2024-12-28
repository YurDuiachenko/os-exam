package com.example.domain.domain;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "students")
@Accessors(chain = true)
public class Student {
    @Id
    private String id;
    private String firstName;
    private String lastName;
    private int age;
    private String major;
}