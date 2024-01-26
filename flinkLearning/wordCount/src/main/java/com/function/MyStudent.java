package com.function;

import com.modle.Student;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;

import java.util.Random;

/**
 * @author pengshilin
 * @date 2023/5/20 1:06
 */
public class MyStudent extends RichSourceFunction<Student> {
    String[] names = {"peng","chen","jing"};
    Random random = new Random();
    @Override
    public void run(SourceContext<Student> sourceContext) throws Exception {
        while (true){
            sourceContext.collect(new Student(names[random.nextInt(names.length)],System.currentTimeMillis(),1));
            Thread.sleep(1000);

        }
    }

    @Override
    public void cancel() {

    }
}
