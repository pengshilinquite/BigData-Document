package com.waterMark;

import PoPj.Event;
import com.function.Myclick;
import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.HashSet;

/**
 * @author pengshilin
 * @date 2023/5/21 19:16
 */
public class WindowProcessTest {
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
                .process(new UVCountByWindow())
                .print();

         env.execute();
    }
    public static class UVCountByWindow extends ProcessWindowFunction<Event,String,Boolean, TimeWindow>{


        @Override
        public void process(Boolean aBoolean, Context context, Iterable<Event> iterable, Collector<String> collector) throws Exception {
            HashSet<String> userSet = new HashSet<>();
            for (Event event : iterable) {
                userSet.add(event.user);
            }

            int size = userSet.size();
            long start = context.window().getStart();
            long end = context.window().getEnd();
            collector.collect("窗口："+new Timestamp(start)+"~"+new Timestamp(end) + "     UV值:"+size);

        }
    }

}
