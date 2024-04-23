package transform.partition;

import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * @author pengshilin
 * @date 2024/4/9 12:48
 */
public class PartitionDemo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(2);

        DataStreamSource<String> textStream = env.socketTextStream("192.168.149.10", 7777);

        //shuffle分区：random().nextInt(下游并行度)
        textStream.shuffle().print();

        //rebalance轮询：对分区进行轮询，一个一个分区进行发送，可以解决数据倾斜场景
        textStream.rebalance().print();

        //缩放分区：实现轮询分区，局部先进行组队，然后再轮询，比rebalance 更加高效
        textStream.rescale().print();

        //广播分区：发送给下游所有子任务
        textStream.broadcast().print();

        //全局 ：只发往第一个分区，强行让下游只有一个分区  ，实战中用的比较少
        textStream.global().print();

        //keyBy ：按照指定key 去发往到对应的分区

        //one to one :  Forward 分区器

        // 自定义分区器：CustomPartitionerWrapper


        //一共 7种分区器 + 1种自定义分区器

        env.execute();
    }
}
