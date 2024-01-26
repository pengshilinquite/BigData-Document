package com.function;

import PoPj.Event;
import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;

import java.time.Duration;

/**
 * @author pengshilin
 * @date 2023/5/23 1:00
 */
public class processfunctionTest {
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
        eventWaterMark.process(new ProcessFunction<Event, String>() {
            @Override
            public void processElement(Event event, Context context, Collector<String> collector) throws Exception {
                if (event.user.equals("Mary")){
                    collector.collect(event.user+"click"+event.url);
                } else  if (event.user.equals("Bob")){
                    collector.collect(event.user+"click"+event.url);
                }
                collector.collect(event.toString());
                System.out.println("timestamp:" + context.timestamp());
                System.out.println("timestamp:" + context.timerService().currentProcessingTime());
                System.out.println("Watermark:" + context.timerService().currentWatermark());

            }

            @Override
            public void onTimer(long timestamp, OnTimerContext ctx, Collector<String> out) throws Exception {
                super.onTimer(timestamp, ctx, out);
            }

            @Override
            public void open(Configuration parameters) throws Exception {
                super.open(parameters);
            }
        }).print();
        env.execute();
    }
}
