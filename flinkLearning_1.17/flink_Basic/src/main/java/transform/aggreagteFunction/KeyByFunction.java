package transform.aggreagteFunction;

import bean.WaterSensor;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * @author pengshilin
 * @date 2024/4/9 10:56
 */
public class KeyByFunction {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(3);

        DataStreamSource<WaterSensor> fromElements = env.fromElements(
                new WaterSensor("s1", 1L, 1),
                new WaterSensor("s1", 1L, 11),
                new WaterSensor("s2", 2L, 2),
                new WaterSensor("s4", 3L, 3)
        );

        /**
         * keyBy:按照id 进行分组
         *      要点：
         *      1、返回的是 一个KeyedStream流
         *      2.keyby 不是转换算子，只是对数据进行重分区
         */
        KeyedStream<WaterSensor, String> keyedStream = fromElements.keyBy(new KeySelector<WaterSensor, String>() {
            @Override
            public String getKey(WaterSensor waterSensor) throws Exception {
                return waterSensor.getId();
            }
        });

        //传位置索引只能够适用于Tuple类型，不适用于 PoPj 类型
        keyedStream.sum("vc").print();



        env.execute();
    }
}
