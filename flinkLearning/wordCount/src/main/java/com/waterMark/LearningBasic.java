package com.waterMark;

import PoPj.Event;
import org.apache.flink.api.common.eventtime.*;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;

import java.util.Properties;

/**
 * @author pengshilin
 * @date 2023/5/20 23:38
 */
public class LearningBasic {
    public static void main(String[] args) {
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
        //自定义水位线
        eventDataStreamSource.assignTimestampsAndWatermarks(new WatermarkStrategy<Event>() {
            //从数据元素中提取时间戳 生成水位线
            @Override
            public WatermarkGenerator<Event> createWatermarkGenerator(WatermarkGeneratorSupplier.Context context) {
                return null;
            }

            //从数据元素中提取时间戳
            @Override
            public TimestampAssigner<Event> createTimestampAssigner(TimestampAssignerSupplier.Context context) {
                return null;
            }
        });
    }
}
