package transform.split;

import bean.WaterSensor;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

/**
 * @author pengshilin
 * @date 2024/4/9 13:39
 */
public class SideOutputDemo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        DataStreamSource<String> textStream = env.socketTextStream("192.168.149.10", 7777);
        // 将输入类型转为WaterSensor 类型
        SingleOutputStreamOperator<WaterSensor> sensorStream = textStream.map(new MapFunction<String, WaterSensor>() {
            @Override
            public WaterSensor map(String record) throws Exception {
                String[] value = record.split(" ");
                return new WaterSensor(value[0], Long.valueOf(value[1]), Integer.valueOf(value[2]));
            }
        });



        /**
         * 创建OutPutTag 侧输出流
         * 第一个参数：标签名
         * 第二个参数： 放入侧输出流中的数据类型 TypeInformation
         */
        OutputTag<WaterSensor> s1Tag = new OutputTag<>("s1", Types.POJO(WaterSensor.class));
        OutputTag<WaterSensor> s2Tag = new OutputTag<>("s2", Types.POJO(WaterSensor.class));


        //使用侧输出流实现分流
        SingleOutputStreamOperator<WaterSensor> process = sensorStream.process(new ProcessFunction<WaterSensor, WaterSensor>() {
            @Override
            public void processElement(WaterSensor value, Context ctx, Collector<WaterSensor> out) throws Exception {
                String id = value.getId();
                /**
                 * 如果  id 为 "S1"  放到侧输出流 S1中
                 * 如果  id 为 “S2”  放到侧输出流S2 中
                 * 如果 id  为 其他  放到主流中
                 */

                if ("s1".equalsIgnoreCase(id)) {

                    /**
                     * 上下文调用
                     * 第一个参数:Tag对象
                     * 第二个参数：放入侧输出流的数据
                     */
                    ctx.output(s1Tag, value);
                } else if ("s2".equalsIgnoreCase(id)) {
                    ctx.output(s2Tag, value);
                } else {
                    out.collect(value);
                }

            }
        });
        process.print("主流");  //打印的是主流

        process.getSideOutput(s1Tag).print("侧流S1");
        process.getSideOutput(s2Tag).print("侧流S2");
        env.execute();
    }
}
