package transform.transformFunction;

import bean.WaterSensor;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;

/**
 * @author pengshilin
 * @date 2024/4/8 16:28
 */
public class FlatMapFunctionDemo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        DataStreamSource<WaterSensor> fromElements = env.fromElements(
                new WaterSensor("s1", 1L, 1),
                new WaterSensor("s1", 1L, 11),
                new WaterSensor("s2", 2L, 2),
                new WaterSensor("s3", 3L, 3)
        );

        /**
         * FlatMapFunction
         * 对于s1  一进一出
         * 对于S2  一进二出
         * 对于s3   一进零出
         *
         * map 用的是return 方式  控制一进一出
         * FlatMap  通过Collector采集  ，调用一次采集一次，可以一进多出
         */
        fromElements.flatMap(new FlatMapFunction<WaterSensor, String>() {
            @Override
            public void flatMap(WaterSensor waterSensor, Collector<String> collector) throws Exception {
                if ("s1".equalsIgnoreCase(waterSensor.getId())){
                    collector.collect(waterSensor.getVc().toString());
                }else if ("s2".equalsIgnoreCase(waterSensor.getId())){
                    collector.collect(waterSensor.getTs().toString());
                    collector.collect(waterSensor.getVc().toString());
                }
            }
        }).print();



        env.execute();
    }
}
