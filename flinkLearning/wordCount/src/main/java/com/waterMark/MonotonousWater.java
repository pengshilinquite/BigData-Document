package com.waterMark;

import PoPj.Event;
import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.EventTimeSessionWindows;
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;

import java.time.Duration;

/**
 * @author pengshilin
 * @date 2023/5/21 0:04
 */
public class MonotonousWater {
    //创建一个有序流的水位线
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        //每隔100ms触发一次水位线机制，默认的是200ms
        env.getConfig().setAutoWatermarkInterval(100);
        //连接kafka
//        Properties properties = new Properties();
//        properties.setProperty("bootstrap.servers","192.168.149.10:9092");
//        properties.setProperty("group_id","test-consumer-group");
//        properties.setProperty("key.deserializer","org.apache.kafka.common.serialization.StringDeserializer");
//        properties.setProperty("value.deserializer","org.apache.kafka.common.serialization.StringDeserializer");
//        properties.setProperty("auto.offset.rest","latest");
//        DataStreamSource<String> clicksDataSource = env.addSource(new FlinkKafkaConsumer<String>(
//                "clicks"
//                , new SimpleStringSchema()
//                , properties
//        ));
        DataStreamSource<Event> eventDataStreamSource = env.fromElements(
                new Event("Mary", "./home", 1000L),
                new Event("Bob", "./cart", 2000L),
                new Event("Mary", "./cart", 4000L),
                new Event("Bob", "./home", 5000L),
                new Event("Bob", "./cart", 2000L),
                new Event("Bob", "./cart", 6000L)
        );
        //有序流 水位线
//        eventDataStreamSource.assignTimestampsAndWatermarks(WatermarkStrategy.<Event>forMonotonousTimestamps()
//                .withTimestampAssigner(new SerializableTimestampAssigner<Event>() {
//                    @Override
//                    public long extractTimestamp(Event event, long l) {
//                        //这里是毫秒，如果想要秒需要*1000
//                        return event.timestamp * 1000;
//                    }
//                })
//        );

        //乱序流 水位线
        SingleOutputStreamOperator<Event> streamWater = eventDataStreamSource.assignTimestampsAndWatermarks(WatermarkStrategy.<Event>forBoundedOutOfOrderness(
                //针对乱序流插入水位线，延迟时间设置2s
                Duration.ofSeconds(2)
                ).withTimestampAssigner(new SerializableTimestampAssigner<Event>() {
                    @Override
                    public long extractTimestamp(Event event, long l) {
                        return event.timestamp;
                    }
                })

        );
        //streamWater.keyBy(data->data.user)
        //           .countWindow(10,5)  //数量窗口
                   //.window(EventTimeSessionWindows.withGap(Time.minutes(10)))  //一个10分钟的 会话窗口
                   //.window(SlidingEventTimeWindows.of(Time.hours(1),Time.seconds(30))) //滑动时间窗口
                  // .window(TumblingEventTimeWindows.of(Time.hours(1)))  //滚动时间窗口


        env.execute();
    }
}
