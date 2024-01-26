package com.DataSource;

import PoPj.Event;
import com.function.MyRichFuction;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * @author pengshilin
 * @date 2023/5/18 0:13
 */
public class TransformRichFunction {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        //env.setParallelism(2);
        DataStreamSource<Event> event = env.fromElements(

                new Event("Mary", "./home", 1000L),
                new Event("Bob", "./cart", 2000L),
                new Event("Mary", "./prod?id=100", 4000L),
                new Event("Bob", "./home", 5000L),
                new Event("Bob", "./prod?id=10", 2000L),
                new Event("Bob", "./cart", 6000L)
        );
        event.map(new MyRichFuction()).print();
        env.execute();
    }
}
