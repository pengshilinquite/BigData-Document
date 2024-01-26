package com.Sink;

import PoPj.Event;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcSink;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * @author pengshilin
 * @date 2023/5/20 2:12
 */
public class SinkToMysql {
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
//        String sql = "insert into clicks (user,url,timestamp) values (?,?,?)";
//        eventDataStreamSource.addSink(JdbcSink.sink(
//                sql
//                ,((preparedStatement, event) -> {
//                    preparedStatement.setString(1,event.user);
//                    preparedStatement.setString(2,event.url);
//                    preparedStatement.setLong(3,event.timestamp);
//
//
//                })
//                ,new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
//                        .withUrl("jdbc:mysql://192.168.149.10:3306/test")
//                        .withDriverName("com.mysql.jdbc.Driver")
//                        .withUsername("root")
//                        .withPassword("123")
//                .build()
//
//
//                )
                eventDataStreamSource.addSink(JdbcSink.sink(
                        "insert into clicks (user,url,num) values(?,?,?)"
                        ,(preparedStatement, event) -> {
                            preparedStatement.setString(1,event.user);
                            preparedStatement.setString(2,event.url);
                            preparedStatement.setLong(3,event.timestamp);
                        }
                        ,new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
                        .withDriverName("com.mysql.jdbc.Driver")
                        .withUsername("root")
                        .withUrl("jdbc:mysql://192.168.149.10:3306/test")
                        .withPassword("123")
                        .build()

                )

        );
        env.execute();
    }
}
