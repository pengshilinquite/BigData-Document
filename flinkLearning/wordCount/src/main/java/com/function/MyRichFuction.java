package com.function;

import PoPj.Event;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.configuration.Configuration;

/**
 * @author pengshilin
 * @date 2023/5/18 0:18
 */
public class MyRichFuction extends RichMapFunction<Event, Integer> {
    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        System.out.println("open生命周期被调用:"+getRuntimeContext().getIndexOfThisSubtask());
    }

    @Override
    public Integer map(Event event) throws Exception {
        return event.url.length();
    }

    @Override
    public void close() throws Exception {
        super.close();
        System.out.println("open生命周期被调用:"+getRuntimeContext().getIndexOfThisSubtask());
    }



}
