package com.example.domain.grpc;

import com.example.domain.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import student.StudentOuterClass;
import student.StudentServiceGrpc;

@Component
@RequiredArgsConstructor
public class StudentGrpcService extends StudentServiceGrpc.StudentServiceImplBase {
    private final StudentRepository studentRepository;
    private final ModelMapper modelMapper;

    @Override
    public void getAllStudents(StudentOuterClass.Empty request, io.grpc.stub.StreamObserver<StudentOuterClass.StudentList> responseObserver) {
        var students = studentRepository.findAll().stream()
                .map(student -> StudentOuterClass.Student.newBuilder()
                        .setId(student.getId())
                        .setFirstName(student.getFirstName())
                        .setLastName(student.getLastName())
                        .setAge(student.getAge())
                        .setSpecialty(student.getMajor())
                        .build())
                .toList();

        StudentOuterClass.StudentList studentList = StudentOuterClass.StudentList.newBuilder()
                .addAllStudents(students)
                .build();

        responseObserver.onNext(studentList);
        responseObserver.onCompleted();
    }

    @Override
    public void getStudentById(StudentOuterClass.StudentRequest request, io.grpc.stub.StreamObserver<StudentOuterClass.Student> responseObserver) {
        var student = modelMapper.map(studentRepository.findById(request.getId()), StudentOuterClass.Student.class);

        responseObserver.onNext(student);
        responseObserver.onCompleted();
    }
}