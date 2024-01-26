package com.waterMark;

import PoPj.Event;
import com.function.Myclick2;
import com.modle.UrlViewCount;
import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.triggers.CountTrigger;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;


import java.time.Duration;
import java.util.Calendar;
import java.util.Random;

/**
 * @author pengshilin
 * @date 2023/5/22 20:38
 */
public class WindowLater2 {


    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        DataStreamSource<Event> eventDataStreamSource = env.addSource(new SourceFunction<Event>() {
            String [] users = {"Mary"};
            String [] urls = {"./home"};
            int [] num = {1,2,3,4,5,6,7,8};
            //first> UrlViewCount{url='./home', count=1, start=2023-05-22 23:12:04.0, end=2023-05-22 23:12:05.0}
            Random random = new Random();
            boolean run = true;
            @Override
            public void run(SourceContext<Event> sourceContext) throws Exception {
                int i = 0;
                while (i<num.length){
                    sourceContext.collect(new Event(
                            users[random.nextInt(users.length)],
                            urls[random.nextInt(urls.length)],
                            1684768325249L-num[i]*1000l


                    ));
                    i++;
                    Thread.sleep(1000);
                }
            }

            @Override
            public void cancel() {
                run = false;
            }
        });
        SingleOutputStreamOperator<Event> eventWaterMark = eventDataStreamSource.assignTimestampsAndWatermarks(WatermarkStrategy.<Event>forBoundedOutOfOrderness(
                Duration.ofSeconds(4)
                ).withTimestampAssigner(new SerializableTimestampAssigner<Event>() {
                    @Override
                    public long extractTimestamp(Event event, long l) {
                        return event.timestamp;
                    }
                })
        );
        OutputTag<Event> later = new OutputTag<Event>("later"){};
        SingleOutputStreamOperator<UrlViewCount> streamOperator = eventWaterMark.keyBy(data -> data.url)
                .window(TumblingEventTimeWindows.of(Time.seconds(2)))


                .allowedLateness(Time.seconds(6))
                .sideOutputLateData(later)
                .aggregate(new CountAgg(), new CountResultProce());
        streamOperator.print("first");
        streamOperator.getSideOutput(later).print("later");
        env.execute();
    }



    public static class CountAgg implements AggregateFunction<Event,Long,Long> {
        @Override
        public Long createAccumulator() {
            return 0L;
        }

        @Override
        public Long add(Event event, Long aLong) {
            return aLong+1;
        }

        @Override
        public Long getResult(Long aLong) {
            return aLong;
        }

        @Override
        public Long merge(Long aLong, Long acc1) {
            return null;
        }
    }

    public static class CountResultProce extends ProcessWindowFunction<Long, UrlViewCount, String, TimeWindow> {
        @Override
        public void process(String url, Context context, Iterable<Long> iterable, Collector<UrlViewCount> collector) throws Exception {
            System.out.println();
            long start = context.window().getStart();
            long end = context.window().getEnd();
            long count = iterable.iterator().next();
            collector.collect(new UrlViewCount(url,count,start,end));

        }
    }
}
