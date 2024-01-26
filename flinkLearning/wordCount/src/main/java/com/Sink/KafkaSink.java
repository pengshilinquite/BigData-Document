package com.Sink;

import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;

import java.util.Properties;

/**
 * @author pengshilin
 * @date 2023/5/20 1:32
 */
public class KafkaSink {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "192.168.149.10:9092");
        properties.setProperty("group.id", "test-consumer-group");
        properties.setProperty("key.deserializer","org.apache.kafka.common.serialization.StringDeserializer");
        properties.setProperty("value.deserializer","org.apache.kafka.common.serialization.StringDeserializer");
        properties.setProperty("auto.offset.reset", "latest");
        DataStreamSource<String> clicks = env.addSource(new FlinkKafkaConsumer<String>(
                "clicks"
                , new SimpleStringSchema()
                , properties
        ));
        clicks.addSink(new FlinkKafkaProducer<String>(
                "clickProducer"
                ,new SimpleStringSchema()
                ,properties
        ));
        env.execute();

    }
}
