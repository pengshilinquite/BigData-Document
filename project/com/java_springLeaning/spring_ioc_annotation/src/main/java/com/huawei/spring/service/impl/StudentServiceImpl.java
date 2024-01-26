package com.huawei.spring.service.impl;

import com.huawei.spring.dao.StudentDao;
import com.huawei.spring.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("studentService")
public class StudentServiceImpl implements StudentService {
    @Autowired
    private StudentDao studentDao;
    public void saveStudent() {
        studentDao.saveStudent();
    }
}
