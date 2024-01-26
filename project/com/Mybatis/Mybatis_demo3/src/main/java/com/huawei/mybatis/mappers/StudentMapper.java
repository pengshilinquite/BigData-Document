package com.huawei.mybatis.mappers;

import com.huawei.mybatis.popj.Student;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface StudentMapper {
    /**
     * 查询所有的学生信息
     */
    List<Student> selectAllStudent();

    /**
     * 查询学生及学生的班级
     */
    Student getStudent(@Param("sid") Integer sid);

    Student getStudentClassByStepOne(@Param("sid") Integer sid);

    List<Student> getStudentByStepOne(@Param("sid") Integer sid);




}
