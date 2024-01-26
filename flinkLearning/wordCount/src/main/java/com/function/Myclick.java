package com.function;

import PoPj.Event;
import org.apache.flink.streaming.api.functions.source.SourceFunction;

import java.util.Calendar;
import java.util.Random;

/**
 * @author pengshilin
 * @date 2023/5/17 0:38
 */
public class Myclick implements SourceFunction<Event> {

    String [] users = {"Bob","Mary","Alice","Cary"};
    String [] urls = {"./home","./cart","./prod?id=100","./prod?id=10","./fav"};
    Random random = new Random();
    boolean run = true;
    @Override
    public void run(SourceContext<Event> sourceContext) throws Exception {
        while (run){
            sourceContext.collect(new Event(
                    users[random.nextInt(users.length)],
                    urls[random.nextInt(urls.length)],
                    Calendar.getInstance().getTimeInMillis()
                    ));
            Thread.sleep(1000);
        }
    }

    @Override
    public void cancel() {
        run = false;

    }
}
