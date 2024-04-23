package transform.combine;

import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * @author pengshilin
 * @date 2024/4/9 14:27
 */
public class UnionDemo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        DataStreamSource<Integer> source1 = env.fromElements(1, 2, 3, 4, 5);
        DataStreamSource<Integer> source2 = env.fromElements(11, 22, 33, 44, 55);
        DataStreamSource<String> source3 = env.fromElements("a","b","c","d","e");
        DataStreamSource<Integer> source4 = env.fromElements(10, 20, 30, 40);

        /**
         * 只能相同类型的流可以合并
         */
        source1.union(source2).print();
        source1.union(source2).union(source4).print();
        source1.union(source2,source4).print();

        env.execute();
    }
}
