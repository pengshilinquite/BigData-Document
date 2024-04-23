package transform.transformFunction;

import bean.WaterSensor;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * @author pengshilin
 * @date 2024/4/8 15:41
 */
public class MapFunctionDemo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        DataStreamSource<WaterSensor> fromElements = env.fromElements(
                new WaterSensor("s1", 1L, 1),
                new WaterSensor("s2", 2L, 2),
                new WaterSensor("s3", 3L, 3)
        );

        //top方式一 ：  匿名函数   map算子， 一进一出
        fromElements.map(new MapFunction<WaterSensor, String>() {
            @Override
            public String map(WaterSensor waterSensor) throws Exception {
                return waterSensor.getId();
            }
        }).print();

        //TOP方式二 ：lambda 表达式
        fromElements.map(waterSensor -> waterSensor.getId()).print();

        //TOP 方式三 ：定义一个mapFunction
        fromElements.map(new MyMapFunction()).print();


        env.execute();
    }

    public static class MyMapFunction implements org.apache.flink.api.common.functions.MapFunction<WaterSensor,String> {

        @Override
        public String map(WaterSensor value) throws Exception {
            return value.getId();
        }
    }
}
