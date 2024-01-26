package com.DataSource;

import PoPj.Event;
import com.function.Myclick;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * @author pengshilin
 * @date 2023/5/17 0:36
 */
public class CustomizeSource {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        DataStreamSource<Event> eventDataStreamSource = env.addSource(new Myclick());
        SingleOutputStreamOperator<String> map = eventDataStreamSource.map(data -> data.user);
        SingleOutputStreamOperator<String> mapEvent = eventDataStreamSource.map(new CustomizeMapFunction());

        SingleOutputStreamOperator<String> mary = map.filter(
                data -> data.equalsIgnoreCase("Mary")


        );
        SingleOutputStreamOperator<Tuple2<String, Integer>> mapKv = mapEvent.map(
                data -> {
                    return new Tuple2<>(data, 1);
                }
        ).returns(Types.TUPLE(Types.STRING, Types.INT));
        mapKv.keyBy(data->data.f0).reduce(new ReduceFunction<Tuple2<String, Integer>>() {
            @Override
            public Tuple2<String, Integer> reduce(Tuple2<String, Integer> value1, Tuple2<String, Integer> value2) throws Exception {
                return new Tuple2<>(value1.f0,value1.f1+value2.f1);
            }
        }).print();




//        eventDataStreamSource.keyBy(new KeySelector<Event, String>() {
//            @Override
//            public String getKey(Event event) throws Exception {
//                return event.user;
//            }
//        }).max("timestamp").print();


        env.execute();
    }

    public static class  CustomizeMapFunction implements MapFunction<Event,String>{
        @Override
        public String map(Event event) throws Exception {
            return event.user;
        }
    }


}
