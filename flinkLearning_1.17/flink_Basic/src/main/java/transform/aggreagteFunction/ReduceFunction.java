package transform.aggreagteFunction;

import bean.WaterSensor;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * @author pengshilin
 * @date 2024/4/9 11:21
 */
public class ReduceFunction {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
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
        KeyedStream<WaterSensor, String> sensorKeyedStream = fromElements.keyBy(new KeySelector<WaterSensor, String>() {
            @Override
            public String getKey(WaterSensor waterSensor) throws Exception {
                return waterSensor.getId();
            }
        });

        /**
         * reduce ：逻辑非常灵活，只需要保证 输入类型 和 输出类型一致
         *      1、keyBy 之后才能调用
         *      2、输入类型=输出类型，类型不能变
         *      3、两两相加
         */
        SingleOutputStreamOperator<WaterSensor> sensorRecudeStream = sensorKeyedStream.reduce(new org.apache.flink.api.common.functions.ReduceFunction<WaterSensor>() {
            @Override
            public WaterSensor reduce(WaterSensor t1, WaterSensor t2) throws Exception {
                return new WaterSensor(t1.getId(), t2.getTs(), t1.getVc() + t2.getVc());
            }
        });
        sensorRecudeStream.print();

        env.execute();
    }
}
