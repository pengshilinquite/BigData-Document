package com.waterMark;

import PoPj.Event;
import com.function.Myclick;
import com.modle.UrlViewCount;
import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.util.Collector;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author pengshilin
 * @date 2023/5/23 23:45
 */
public class TopNUrl2 {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        DataStreamSource<Event> eventDataStreamSource = env.addSource(new Myclick());
        SingleOutputStreamOperator<Event> eventStream = eventDataStreamSource.assignTimestampsAndWatermarks(WatermarkStrategy.<Event>forBoundedOutOfOrderness(
                Duration.ofSeconds(2)
                ).withTimestampAssigner(new SerializableTimestampAssigner<Event>() {
                    @Override
                    public long extractTimestamp(Event event, long l) {
                        return event.timestamp;
                    }
                })
        );
        //直接开窗，收集所有数据排序
        SingleOutputStreamOperator<UrlViewCount> aggregate = eventStream.keyBy(data -> data.url)
                .window(SlidingEventTimeWindows.of(Time.seconds(10), Time.seconds(5)))
                .aggregate(new WindowLater2.CountAgg(), new WindowLater2.CountResultProce());


        //

        aggregate.keyBy(data->data.end)
                .process(new TopNProcessResulte(2))
                .print();
        env.execute();
    }


    private static class TopNProcessResulte extends KeyedProcessFunction<Long,UrlViewCount,String> {
        private Integer n;

        public TopNProcessResulte(int i) {
            this.n  =i;
        }

        public ListState<UrlViewCount> urlViewCountListState;

        //从运行环境中获取状态


        @Override
        public void open(Configuration parameters) throws Exception {

            urlViewCountListState = getRuntimeContext().getListState(
                    new ListStateDescriptor<UrlViewCount>("url-count", Types.GENERIC(UrlViewCount.class))
            );
        }

        @Override
        public void processElement(UrlViewCount urlViewCount, Context context, Collector<String> collector) throws Exception {
                urlViewCountListState.add(urlViewCount);
                context.timerService().registerProcessingTimeTimer(context.getCurrentKey()+1);
        }

        @Override
        public void onTimer(long timestamp, OnTimerContext ctx, Collector<String> out) throws Exception {
            ArrayList<UrlViewCount> result = new ArrayList<>();
            for (UrlViewCount urlViewCount:urlViewCountListState.get()){
                result.add(urlViewCount);
            }
            result.sort(new Comparator<UrlViewCount>() {
                @Override
                public int compare(UrlViewCount o1, UrlViewCount o2) {
                    return o2.count.intValue() -o1.count.intValue();
                }
            });
            StringBuilder resutl = new StringBuilder();
            resutl.append("=========");
            resutl.append("窗口结束时间: " + new Timestamp(ctx.getCurrentKey()) +"\n");

            for (int i = 0; i < 2; i++) {
                UrlViewCount urlViewCount = result.get(i);
                resutl.append("No. "+ (i+1) + "   " +  "url: " + urlViewCount.url + "访问次数: " + urlViewCount.count+"\n");

            }
            resutl.append("=========");
            out.collect(resutl.toString());
        }
    };
}


