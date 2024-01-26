package com.Multi_Stream_Conversion;

import org.apache.flink.streaming.api.datastream.ConnectedStreams;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.co.CoMapFunction;

/**
 * @author pengshilin
 * @date 2023/5/25 1:40
 */
public class ConnetStream {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        DataStreamSource<Integer> integerDataStreamSource = env.fromElements(1, 2, 3);
        DataStreamSource<String> stringDataStreamSource = env.fromElements("2", "3", "4");
        ConnectedStreams<Integer, String> connect = integerDataStreamSource.connect(stringDataStreamSource);
        SingleOutputStreamOperator<String> outputStreamOperator = connect.map(new CoMapFunction<Integer, String, String>() {
            @Override
            public String map1(Integer value) throws Exception {
                return String.valueOf(value);
            }

            @Override
            public String map2(String value) throws Exception {
                return value;
            }
        });
        outputStreamOperator.print();
        env.execute();
    }
}
