package com.Sink;

import PoPj.Event;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author pengshilin
 * @date 2023/5/20 13:21
 */
public class SinkToMysql2 extends RichSinkFunction<Event> {
    PreparedStatement ps;
    private Connection connection;


    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        if (connection == null) {
            connection = getConnection();
        }

        System.out.println(connection);
        String sql = "insert into clicks (user,url,timestamp ) values(?,?,?)";
        if (connection!=null){
            ps=this.connection.prepareStatement(sql);
        }


    }

    @Override
    public void invoke(Event value, Context context) throws Exception {
        ps.setString(1,value.user);
        ps.setString(2,value.url);
        ps.setLong(3,value.timestamp);
        ps.executeUpdate();

    }

    @Override
    public void close() throws Exception {
        super.close();
        if (connection !=null){
            connection.close();
        }
        if (ps!=null){
            ps.close();
        }
    }

    private static Connection getConnection(){
        Connection connection = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection(
                    "jdbc:mysql://192.168.149.10:3306/test"
                    ,"root"
                    ,"123"

            );
        } catch (ClassNotFoundException e) {
            System.out.println("errror======================");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return connection;
    }

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        DataStreamSource<Event> eventDataStreamSource = env.fromElements(
                new Event("Mary", "./home", 1000L),
                new Event("Bob", "./cart", 2000L),
                new Event("Mary", "./cart", 4000L),
                new Event("Bob", "./home", 5000L),
                new Event("Bob", "./cart", 2000L),
                new Event("Bob", "./cart", 6000L)
        );
       eventDataStreamSource.addSink(new SinkToMysql2());
       env.execute();
    }
}
