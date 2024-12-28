package com.example.domain.rabbitmq;

import com.example.domain.domain.Student;
import com.example.domain.repository.StudentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StudentEventListener {
    private final ObjectMapper objectMapper;
    private final StudentRepository studentRepository;

    @RabbitListener(queues = "studentCreated")
    public void listen(String massage) {
        Student student;
        try {
            System.out.println(massage);
            student = objectMapper.readValue(massage, Student.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        studentRepository.save(student);
    }
}