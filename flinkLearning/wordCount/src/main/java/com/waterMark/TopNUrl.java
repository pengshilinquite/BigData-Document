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
import org.apache.flink.streaming.api.functions.windowing.ProcessAllWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import java.security.Key;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

/**
 * @author pengshilin
 * @date 2023/5/23 20:57
 */
public class TopNUrl {
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
        eventStream.map(data->data.url)
                .windowAll(SlidingEventTimeWindows.of(Time.seconds(10),Time.seconds(5)))
                .aggregate(new UrlCountAgg(),new UrlResultProcess())
                .print();


        //
        env.execute();
    }

    private static class UrlCountAgg implements AggregateFunction<String, HashMap<String,Long>, ArrayList<Tuple2<String,Long>>> {

        @Override
        public HashMap<String, Long> createAccumulator() {
            return new HashMap<>();
        }

        @Override
        public HashMap<String, Long> add(String url, HashMap<String, Long> stringLongHashMap) {
            if (stringLongHashMap.containsKey(url)){
                Long count = stringLongHashMap.get(url);
                stringLongHashMap.put(url,count+1l);
            }else {
                stringLongHashMap.put(url,1L);
            }
            return stringLongHashMap;
        }

        @Override
        public ArrayList<Tuple2<String, Long>> getResult(HashMap<String, Long> stringLongHashMap) {
            ArrayList<Tuple2<String, Long>> result = new ArrayList<>();
            for (String key : stringLongHashMap.keySet()) {
                result.add(Tuple2.of(key,stringLongHashMap.get(key)));
            }
            result.sort(new Comparator<Tuple2<String, Long>>() {
                @Override
                public int compare(Tuple2<String, Long> o1, Tuple2<String, Long> o2) {
                    return o2.f1.intValue() - o1.f1.intValue();
                }
            });
            return result;
        }

        @Override
        public HashMap<String, Long> merge(HashMap<String, Long> stringLongHashMap, HashMap<String, Long> acc1) {
            return null;
        }
    }

    private static class UrlResultProcess extends ProcessAllWindowFunction<ArrayList<Tuple2<String,Long>>,String, TimeWindow> {


        @Override
        public void process(Context context, Iterable<ArrayList<Tuple2<String, Long>>> iterable, Collector<String> collector) throws Exception {
            StringBuilder resutl = new StringBuilder();
            resutl.append("=========");
            resutl.append("窗口结束时间: " + new Timestamp(context.window().getEnd()) +"\n");
            ArrayList<Tuple2<String, Long>> list = iterable.iterator().next();
            for (int i = 0; i < 2; i++) {
                Tuple2<String, Long> stringLongTuple2 = list.get(i);
                resutl.append("No. "+ (i+1) + "   " +  "url: " + stringLongTuple2.f0 + "访问次数: " + stringLongTuple2.f1+"\n");

            }
            resutl.append("=========");
            collector.collect(resutl.toString());

        }
    }
}
