package com.huawei.mybatis.test;

import com.huawei.mybatis.mappers.ClassMapper;
import com.huawei.mybatis.mappers.StudentMapper;
import com.huawei.mybatis.popj.Class;
import com.huawei.mybatis.popj.Student;
import com.huawei.mybatis.util.SqlSessionUtil;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class TestStudent {
        @Test
    public void test() throws IOException {
            SqlSession sqlSession = SqlSessionUtil.getSqlSession();
            System.out.println(sqlSession);
            StudentMapper mapperStudent = sqlSession.getMapper(StudentMapper.class);
//            List<Student> students = mapper.selectAllStudent();
//            students.forEach(stu-> System.out.println(stu));
//            Student student = mapper.getStudent(1);
//            System.out.println(student);
            //ClassMapper mapperClass = sqlSession.getMapper(ClassMapper.class);

            Student studentClassByStepOne = mapperStudent.getStudentClassByStepOne(1);
            System.out.println(studentClassByStepOne.getStudentName());

            ClassMapper classMapper = sqlSession.getMapper(ClassMapper.class);
            Class aClass = classMapper.getClass(1);

            System.out.println(aClass);

            System.out.println("+++++++++++++");
            Class studentByStepOne = classMapper.getStudentByStepOne(1);
            System.out.println(studentByStepOne);


        }
        @Test
        public void test2() throws ClassNotFoundException, SQLException {
                java.lang.Class.forName("com.mysql.jdbc.Driver");
                Connection root = DriverManager.getConnection("jdbc:mysql://192.168.31.182:3306", "root", "123");
                System.out.println(root);

        }


}
