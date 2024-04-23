package transform.partition;

import org.apache.flink.api.common.functions.Partitioner;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * @author pengshilin
 * @date 2024/4/9 13:10
 * 自定义分区器
 */
public class PartitionCustomDemo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(2);

        DataStreamSource<String> textStream = env.socketTextStream("192.168.149.10", 7777);

        DataStream<String> stringDataStream = textStream.partitionCustom(
                new Partitioner<String>() {
                    @Override
                    public int partition(String key, int numPartition) {
                        return Integer.parseInt(key) % numPartition;
                    }
                }
                ,
                new KeySelector<String, String>() {
                    @Override
                    public String getKey(String value) throws Exception {
                        return value;
                    }
                });
        stringDataStream.print();

        env.execute();
    }
}
