package com.waterMark;

import com.function.MyStudent;
import com.modle.Student;
import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;

import java.sql.Timestamp;
import java.time.Duration;

/**
 * @author pengshilin
 * @date 2023/5/21 17:44
 */
public class WindowAggregateTest {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        DataStreamSource<Student> studentDataStreamSource = env.addSource(new MyStudent());
        env.setParallelism(1);
        SingleOutputStreamOperator<Student> studentWaterMark = studentDataStreamSource.assignTimestampsAndWatermarks(WatermarkStrategy.<Student>forBoundedOutOfOrderness(
                Duration.ofSeconds(1)

                ).withTimestampAssigner(new SerializableTimestampAssigner<Student>() {
                    @Override
                    public long extractTimestamp(Student student, long l) {
                        return student.timestamp;
                    }
                })
        );
/*        //利用reduce 实现开窗累加
        studentDataStreamSource.keyBy(data->data.name).reduce(
                new ReduceFunction<Student>() {
                    @Override
                    public Student reduce(Student student, Student t1) throws Exception {
                        return new Student(student.name,t1.timestamp,student.num+t1.num);
                    }
                }
        ).print();*/

        studentWaterMark.keyBy(data->data.name)
                .window(TumblingEventTimeWindows.of(Time.seconds(5)))
                .aggregate(new AggregateFunction<Student, Tuple2<Long,Integer>, String>() {
                    @Override
                    public Tuple2<Long, Integer> createAccumulator() {
                        return Tuple2.of(0L,0);
                    }

                    @Override
                    public Tuple2<Long, Integer> add(Student student, Tuple2<Long, Integer> accumul) {
                        return Tuple2.of(accumul.f1+student.timestamp,accumul.f1+1);
                    }

                    @Override
                    public String getResult(Tuple2<Long, Integer> accumul) {
                        Timestamp timestamp = new Timestamp(accumul.f0 / accumul.f1);
                        return timestamp.toString();
                    }

                    @Override
                    public Tuple2<Long, Integer> merge(Tuple2<Long, Integer> value1, Tuple2<Long, Integer> value2) {
                        return new Tuple2<>(value1.f0+value2.f0,value1.f1+value2.f1);
                    }
                }).print();
        env.execute();
    }


}
