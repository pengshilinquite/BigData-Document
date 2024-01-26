package com.DataSource;

import PoPj.Event;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;
import scala.Int;

/**
 * @author pengshilin
 * @date 2023/5/18 0:35
 */
public class TransformPartitionTest {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(2);
        DataStreamSource<Event> event = env.fromElements(

                new Event("Mary", "./home", 1000L),
                new Event("Bob", "./cart", 2000L),
                new Event("Mary", "./prod?id=100", 4000L),
                new Event("Bob", "./home", 5000L),
                new Event("Bob", "./prod?id=10", 2000L),
                new Event("Bob", "./cart", 6000L)
        );
        //随机分区
        //event.shuffle().print("随机").setParallelism(3);
        //轮询分区
        event.rebalance().print("轮询").setParallelism(3);
        
        
        env.addSource(new RichParallelSourceFunction<Integer>() {
            @Override
            public void run(SourceContext<Integer> sourceContext) throws Exception {
                for (int i = 0; i <= 8; i++) {
                    if (i%2==getRuntimeContext().getIndexOfThisSubtask()){
                        sourceContext.collect(i);
                    }
                }
            }

            @Override
            public void cancel() {

            }
        }).setParallelism(2).rescale().print().setParallelism(4);

        env.execute();
    }
}
