package com.controller;


import com.service.StudentService;

import static com.service.StudentService.*;

public class StudentController {
    private StudentService studentService;

    public StudentService getStudentService() {
        return studentService;
    }

    public void setStudentService(StudentService studentService) {
        this.studentService = studentService;
    }

    public void saveStudnt(){
        studentService.saveStudent();
    }

    @Override
    public String toString() {
        return "StudentController{" +
                "studentService=" + studentService +
                '}';
    }
}
