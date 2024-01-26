package com.huawei.mybatis.test;


import com.huawei.mybatis.mapper.UserMapper;
import com.huawei.mybatis.pojo.User;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class MybatisTest {

    @Test
    public void testMyBatis() throws IOException {
        //加载配置文件
        InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
        //获取SqlSessionFactoryBuilder

        SqlSessionFactoryBuilder sqlSessionFactoryBuilder = new SqlSessionFactoryBuilder();

         SqlSessionFactory sqlSessionFactory = sqlSessionFactoryBuilder.build(inputStream);

        //获取sqlSession
        SqlSession sqlSession = sqlSessionFactory.openSession(true);
        //获取Mapper对象
        UserMapper mapper = sqlSession.getMapper(UserMapper.class);
        int result = mapper.insertUser();
        System.out.println("result"+result);
        int i = mapper.updateUser();
        System.out.println(i);



    }
    @Test
    public void testMybatisUpdate() throws IOException {
        InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
        SqlSessionFactoryBuilder sqlSessionFactoryBuilder = new SqlSessionFactoryBuilder();
        SqlSessionFactory build = sqlSessionFactoryBuilder.build(inputStream);
        SqlSession sqlSession = build.openSession(true);
        UserMapper mapper = sqlSession.getMapper(UserMapper.class);
        int i = mapper.updateUser();
        System.out.println(i);


    }

    @Test
    public void testSelect() throws IOException {
        InputStream resourceAsStream = Resources.getResourceAsStream("mybatis-config.xml");
        SqlSessionFactoryBuilder sqlSessionFactoryBuilder = new SqlSessionFactoryBuilder();
        SqlSessionFactory build = sqlSessionFactoryBuilder.build(resourceAsStream);
        SqlSession sqlSession = build.openSession(true);
        UserMapper mapper = sqlSession.getMapper(UserMapper.class);
        User user = mapper.selectUser();
        System.out.println(user);
    }

    @Test
    public void testSelectAll() throws IOException {
        InputStream resourceAsStream = Resources.getResourceAsStream("mybatis-config.xml");
        SqlSessionFactoryBuilder sqlSessionFactoryBuilder = new SqlSessionFactoryBuilder();
        SqlSessionFactory build = sqlSessionFactoryBuilder.build(resourceAsStream);
        SqlSession sqlSession = build.openSession(true);
        UserMapper mapper = sqlSession.getMapper(UserMapper.class);
        List<User> users = mapper.selectAllUser();


        users.forEach(user-> System.out.println(user));
    }

}
