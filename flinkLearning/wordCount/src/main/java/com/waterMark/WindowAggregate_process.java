package com.waterMark;

import PoPj.Event;
import com.function.Myclick;
import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import javax.swing.plaf.IconUIResource;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.HashSet;

/**
 * @author pengshilin
 * @date 2023/5/21 19:43
 */
public class WindowAggregate_process {
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
        eventWaterMark.keyBy(data->true)
                .window(TumblingEventTimeWindows.of(Time.seconds(5)))
                .aggregate(new UVAVGWindow(),new UVCountResult())
                .print();
        env.execute();
    }

    //自定义实现AggregateFunction，增量聚合计算UV值
    private static class UVAVGWindow implements AggregateFunction<Event, HashSet<String>,Long> {
        @Override
        public HashSet<String> createAccumulator() {
            return new HashSet<String>();
        }

        @Override
        public HashSet<String> add(Event event, HashSet<String> strings) {
            strings.add(event.user);
            return strings;
        }

        @Override
        public Long getResult(HashSet<String> strings) {
            return (long) strings.size();
        }

        @Override
        public HashSet<String> merge(HashSet<String> strings, HashSet<String> acc1) {
            return null;
        }
    }

    //自定义实现ProcessionWindowFunciton，包装窗口信息输出
    public static class UVCountResult extends ProcessWindowFunction<Long,String,Boolean, TimeWindow>{

        @Override
        public void process(Boolean b, Context context, Iterable<Long> iterable, Collector<String> collector) throws Exception {
            long start = context.window().getStart();
            long end = context.window().getEnd();
            Long value = iterable.iterator().next();
            collector.collect("窗口："+new Timestamp(start)+"~"+new Timestamp(end) + "     UV值:"+value);


        }
    }
}
