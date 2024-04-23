package source;


import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.util.Arrays;

/**
 * @author pengshilin
 * @date 2024/4/7 15:36
 */
public class CollectSource {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        //从集合中读取元素
        DataStreamSource<Integer> collectSource = env.fromCollection(Arrays.asList(1, 2, 3));
        collectSource.print("collect");

        //直接读取元素
        DataStreamSource<Integer> eleSource = env.fromElements(1, 2, 3);
        eleSource.print("ele");


        env.execute();
    }
}
