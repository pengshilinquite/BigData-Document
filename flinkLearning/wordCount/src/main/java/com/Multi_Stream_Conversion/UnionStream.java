package com.Multi_Stream_Conversion;

import PoPj.Event;
import com.function.Myclick;
import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;

import java.time.Duration;

/**
 * @author pengshilin
 * @date 2023/5/25 1:09
 */
public class UnionStream {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        DataStreamSource<String> eventSource = env.socketTextStream("192.168.149.10", 7777);
        SingleOutputStreamOperator<Event> mapStream = eventSource.map(new MapFunction<String, Event>() {
            @Override
            public Event map(String data) throws Exception {
                String[] split = data.split(",");
                return new Event(split[0], split[1], Long.valueOf(split[2]));
            }
        }).assignTimestampsAndWatermarks(WatermarkStrategy.<Event>forBoundedOutOfOrderness(
                Duration.ofSeconds(2)
        ).withTimestampAssigner(new SerializableTimestampAssigner<Event>() {
                    @Override
                    public long extractTimestamp(Event event, long l) {
                        return event.timestamp;
                    }
                })
        );
        DataStreamSource<String> eventSource2 = env.socketTextStream("192.168.149.10", 8888);
        SingleOutputStreamOperator<Event> mapStream2 = eventSource2.map(new MapFunction<String, Event>() {
            @Override
            public Event map(String data) throws Exception {
                String[] split = data.split(",");
                return new Event(split[0], split[1], Long.valueOf(split[2]));
            }
        }).assignTimestampsAndWatermarks(WatermarkStrategy.<Event>forBoundedOutOfOrderness(
                Duration.ofSeconds(5)
                ).withTimestampAssigner(new SerializableTimestampAssigner<Event>() {
                    @Override
                    public long extractTimestamp(Event event, long l) {
                        return event.timestamp;
                    }
                })
        );
        //mapStream.print("1111");
        //mapStream2.print("2222");

        //合并两条流,查看水位线
//        mapStream.union(mapStream2).process(
//                new ProcessFunction<Event, String>() {
//                    @Override
//                    public void processElement(Event value, Context ctx, Collector<String> out) throws Exception {
//                        out.collect("水位线是"+ctx.timerService().currentWatermark());
//                    }
//                }
//        ).print();
        mapStream.union(mapStream2).print("union");
        env.execute();

    }
}
