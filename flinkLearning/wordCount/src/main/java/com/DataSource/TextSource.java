package com.DataSource;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;

/**
 * @author pengshilin
 * @date 2023/5/16 22:30
 */
public class TextSource {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        DataStreamSource<String> words = env.readTextFile("input/words");
        SingleOutputStreamOperator<String> word = words.flatMap(new FlatMapFunction<String, String>() {
            @Override
            public void flatMap(String wordLine, Collector<String> collector) throws Exception {
                String[] words = wordLine.split(" ");
                for (String word : words) {
                    collector.collect(word);
                }
            }
        }).returns(Types.STRING);
        word.print();
        env.execute();
    }
}
