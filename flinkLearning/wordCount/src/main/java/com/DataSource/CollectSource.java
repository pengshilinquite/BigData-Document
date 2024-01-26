package com.DataSource;

import PoPj.Event;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.util.ArrayList;

/**
 * @author pengshilin
 * @date 2023/5/16 22:47
 */
public class CollectSource {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        ArrayList<Integer> list = new ArrayList<Integer>();
        list.add(1);
        list.add(2);
        DataStreamSource<Integer> listDataSource = env.fromCollection(list);
        listDataSource.print("num");


        ArrayList<Event> events = new ArrayList<Event>();
        events.add(new Event("Mary","./home",1000L));
        events.add(new Event("Bob","./cart",2000L));
        events.add(new Event("Mary","./cart",4000L));
        events.add(new Event("Bob","./home",5000L));
        events.add(new Event("Bob","./cart",2000L));
        events.add(new Event("Bob","./cart",6000L));
        DataStreamSource<Event> eventDataStreamSource = env.fromCollection(events);
       // eventDataStreamSource.print("Event");
        eventDataStreamSource.keyBy(new KeySelector<Event, String>() {
            @Override
            public String getKey(Event event) throws Exception {
                return event.user;
            }
        }).max("timestamp").print();

//        DataStreamSource<Event> eventFromEleDataSource = env.fromElements(
//                new Event("Mary", "./home", 1000L),
//                new Event("Bob", "./cart", 2000L)
//
//        );
//        eventFromEleDataSource.print("fromEle");

        env.execute();
    }
}
