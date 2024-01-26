package com.psl;

import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;

/**
 * @author pengshilin
 * @date 2023/5/14 22:26
 */
public class StreamWordCount {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment executionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment();
        
        System.out.println("executionEnvironment:  "+executionEnvironment.getParallelism());
        //DataStreamSource<String> streamWordLine = executionEnvironment.readTextFile("input/words");
        DataStreamSource<String> streamWordLine = executionEnvironment.socketTextStream("192.168.149.10",7777);
        System.out.println("streamWordLine:  "+streamWordLine.getParallelism());
        SingleOutputStreamOperator<Tuple2<String, Long>> wordFlatMap = streamWordLine.flatMap((String line, Collector<Tuple2<String, Long>> out) -> {
            String[] lineWord = line.split(" ");
            for (String word : lineWord) {
                out.collect(Tuple2.of(word, 1L));
            }
        }).returns(Types.TUPLE(Types.STRING, Types.LONG));
        System.out.println("wordFlatMap:  "+wordFlatMap.getParallelism());

        KeyedStream<Tuple2<String, Long>, Object> wordKeyBy = wordFlatMap.keyBy(data -> data.f0);
        System.out.println("wordKeyBy:  "+wordKeyBy.getParallelism());
        SingleOutputStreamOperator<Tuple2<String, Long>> wordSum = wordKeyBy.sum(1).setParallelism(2);
        System.out.println("wordSum:  "+wordSum.getParallelism());
        wordSum.print();
        executionEnvironment.execute();

    }
}
