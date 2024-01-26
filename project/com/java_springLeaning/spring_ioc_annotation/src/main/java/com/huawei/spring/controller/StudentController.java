package com.huawei.spring.controller;

import com.huawei.spring.dao.StudentDao;
import com.huawei.spring.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

//@Controller("studentController2")
@Controller
public class StudentController {
    @Autowired
    private StudentService studentService;

    public void saveStudent(){
        studentService.saveStudent();
    }
}
