package com.function;

import PoPj.Event;
import org.apache.flink.streaming.api.functions.source.SourceFunction;

import java.util.Calendar;
import java.util.Random;

/**
 * @author pengshilin
 * @date 2023/5/17 0:38
 */
public class Myclick2 implements SourceFunction<Event> {

    String [] users = {"Mary"};
    String [] urls = {"./home"};
    Random random = new Random();
    boolean run = true;
    @Override
    public void run(SourceContext<Event> sourceContext) throws Exception {
        while (run){
            sourceContext.collect(new Event(
                    users[random.nextInt(users.length)],
                    urls[random.nextInt(urls.length)],
                    Calendar.getInstance().getTimeInMillis()-random.nextInt(10)*1000
            ));
            Thread.sleep(1000);
        }
    }

    @Override
    public void cancel() {
        run = false;

    }
}
