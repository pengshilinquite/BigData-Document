package com.peng.learning.Dao;

import com.alibaba.druid.pool.DruidDataSourceFactory;

import javax.sql.DataSource;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.util.Properties;

public class DruidDao {
    //获取数据库连接池
    public Connection getDruidConnect() throws Exception {
        Properties properties = new Properties();
        properties.load(new FileInputStream("src\\main\\java\\com\\peng\\learning\\druid.properties"));
        DataSource dataSource = DruidDataSourceFactory.createDataSource(properties);
        Connection connection = dataSource.getConnection();
        return connection;
    }
}
