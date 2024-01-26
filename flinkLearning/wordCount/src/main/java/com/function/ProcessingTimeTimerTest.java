package com.function;

import PoPj.Event;
import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;

import java.sql.Timestamp;
import java.time.Duration;

/**
 * @author pengshilin
 * @date 2023/5/23 1:24
 */
public class ProcessingTimeTimerTest {
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
        eventWaterMark.keyBy(data->data.user).process(new KeyedProcessFunction<String, Event, String>() {
            @Override
            public void processElement(Event event, Context context, Collector<String> collector) throws Exception {
                long currentProcessingTime = context.timerService().currentProcessingTime();
                collector.collect(context.getCurrentKey()+"数据到达，到达时间："+new Timestamp(currentProcessingTime));
                //注册一个10s 后的定时器
                context.timerService().registerProcessingTimeTimer(currentProcessingTime + 10 *1000L);
            }

            @Override
            public void onTimer(long timestamp, OnTimerContext ctx, Collector<String> out) throws Exception {
                out.collect(ctx.getCurrentKey()+"定时器触发，触发时间：" + new Timestamp(timestamp));
            }
        }).print();
        env.execute();
    }
}
