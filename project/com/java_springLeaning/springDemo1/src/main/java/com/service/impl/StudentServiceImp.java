package com.service.impl;

import com.dao.StudentDao;
import com.service.StudentService;

public class StudentServiceImp implements StudentService {
    private StudentDao studentDao;

    public StudentDao getStudentDao() {
        return studentDao;
    }

    public void setStudentDao(StudentDao studentDao) {
        this.studentDao = studentDao;
    }

    @Override
    public void saveStudent() {
            studentDao.saveStudent();
    }


}
