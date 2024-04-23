package transform.split;

import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 *  对数据按照奇偶进行分流： 缺点： 数据要进行两次处理，可以采用 侧输出流
 * @author pengshilin
 * @date 2024/4/9 13:27
 */
public class SplitStreamByFilter {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        DataStreamSource<String> textStream = env.socketTextStream("192.168.149.10", 7777);
        SingleOutputStreamOperator<Integer> integeStream = textStream.map(value -> Integer.parseInt(value));
        SingleOutputStreamOperator<Integer> filter1 = integeStream.filter(text -> text % 2 == 0);
        SingleOutputStreamOperator<Integer> filter2 = integeStream.filter(text -> text % 2 == 1);

        filter1.print("偶数");
        filter2.print("奇数");

        env.execute();
    }
}
