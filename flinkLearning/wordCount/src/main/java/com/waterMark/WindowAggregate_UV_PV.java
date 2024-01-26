package com.waterMark;

import PoPj.Event;
import com.function.Myclick;
import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;

import java.time.Duration;
import java.util.HashSet;

/**
 * @author pengshilin
 * @date 2023/5/21 18:43
 */
public class WindowAggregate_UV_PV {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        DataStreamSource<Event> eventDataStreamSource = env.addSource(new Myclick());
        SingleOutputStreamOperator<Event> eventWaterMark = eventDataStreamSource.assignTimestampsAndWatermarks(WatermarkStrategy.<Event>forBoundedOutOfOrderness(
                Duration.ofSeconds(2)

                ).withTimestampAssigner(new SerializableTimestampAssigner<Event>() {
                    @Override
                    public long extractTimestamp(Event event, long l) {
                        return event.timestamp;
                    }
                })
        );
        eventDataStreamSource.print();
        //算 pv / uv  平均点击率
        eventWaterMark.keyBy(data->true)
                .window(TumblingEventTimeWindows.of(Time.seconds(5)))
                .aggregate(new AVGPV()).print();
        env.execute();
    }
    //用LONG 保存 pv 个数
    public static class AVGPV implements AggregateFunction<Event, Tuple2<Long, HashSet<String>>,Double>{

        @Override
        public Tuple2<Long, HashSet<String>> createAccumulator() {
            return new Tuple2<>(0L,new HashSet<>());
        }

        @Override
        public Tuple2<Long, HashSet<String>> add(Event event, Tuple2<Long, HashSet<String>> longHashSetTuple2) {
            longHashSetTuple2.f1.add(event.user);
            return new Tuple2<Long,HashSet<String>>(longHashSetTuple2.f0+1,longHashSetTuple2.f1);
        }

        @Override
        public Double getResult(Tuple2<Long, HashSet<String>> longHashSetTuple2) {
            return (double)longHashSetTuple2.f0/longHashSetTuple2.f1.size();
        }

        @Override
        public Tuple2<Long, HashSet<String>> merge(Tuple2<Long, HashSet<String>> longHashSetTuple2, Tuple2<Long, HashSet<String>> acc1) {
            return null;
        }

    }
}
