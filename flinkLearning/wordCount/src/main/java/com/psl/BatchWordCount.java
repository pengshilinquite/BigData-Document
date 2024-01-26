package com.psl;

import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.operators.AggregateOperator;
import org.apache.flink.api.java.operators.DataSource;
import org.apache.flink.api.java.operators.FlatMapOperator;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.util.Collector;

/**
 * @author pengshilin
 * @date 2023/5/14 21:57
 */
public class BatchWordCount {
    public static void main(String[] args) throws Exception {
        ExecutionEnvironment executionEnvironment = ExecutionEnvironment.getExecutionEnvironment();
        DataSource<String> words = executionEnvironment.readTextFile("input/words");
        FlatMapOperator<String, Tuple2<String, Long>> wordFlatMap = words.flatMap((String line, Collector<Tuple2<String, Long>> out) -> {
            String[] lineWord = line.split(" ");
            for (String word : lineWord) {
                out.collect(Tuple2.of(word, 1L));
            }

        }).returns(Types.TUPLE(Types.STRING, Types.LONG));

        AggregateOperator<Tuple2<String, Long>> wordSum = wordFlatMap.groupBy(0).sum(1);

        wordSum.print();



    }
}
