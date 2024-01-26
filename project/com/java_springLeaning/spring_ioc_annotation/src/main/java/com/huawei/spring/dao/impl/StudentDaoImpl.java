package com.huawei.spring.dao.impl;

import com.huawei.spring.dao.StudentDao;
import org.springframework.stereotype.Repository;

@Repository("studentDao")
public class StudentDaoImpl implements StudentDao {
    public void saveStudent() {
        System.out.println("保持成功");
    }
}
