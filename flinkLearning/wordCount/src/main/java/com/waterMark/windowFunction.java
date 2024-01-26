package com.waterMark;

import com.function.MyStudent;
import com.modle.Student;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;

/**
 * @author pengshilin
 * @date 2023/5/21 17:22
 */
public class windowFunction {
    public static void main(String[] args) throws Exception {
                StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
                env.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime);
                env.setParallelism(1);

       DataStreamSource<Student> studentDataStreamSource = env.addSource(new MyStudent());
        SingleOutputStreamOperator<Tuple2<String, Integer>> mapStudent = studentDataStreamSource.map(
                data -> {
                    return new Tuple2<>(data.name, data.num);
                }
        ).returns(Types.TUPLE(Types.STRING,Types.INT));

        mapStudent.keyBy(
                data->data.f0
        ).window(TumblingProcessingTimeWindows.of(Time.seconds(5)))
                .reduce(new ReduceFunction<Tuple2<String, Integer>>() {
            @Override
            public Tuple2<String, Integer> reduce(Tuple2<String, Integer> value1, Tuple2<String, Integer> value2 ) throws Exception {
                return new Tuple2<>(value1.f0,value1.f1+value2.f1);
            }
        }).print();


        env.execute();


    }
}
