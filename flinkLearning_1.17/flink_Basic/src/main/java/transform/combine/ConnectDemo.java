package transform.combine;

import org.apache.flink.streaming.api.datastream.ConnectedStreams;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.co.CoMapFunction;

/**
 * @author pengshilin
 * @date 2024/4/9 14:37
 */
public class ConnectDemo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        DataStreamSource<Integer> source1 = env.fromElements(1, 2, 3, 4, 5);
        DataStreamSource<String> source3 = env.fromElements("a","b","c","d","e");

        /**
         * connect 连接流
         * 一次只能连接2条流
         * 流的数据类型可以不一样
         * 连接后可以用flatMap  Map  process 函数进行处理，但是 各处理各的
         *
         */

        ConnectedStreams<Integer, String> connect = source1.connect(source3);
        SingleOutputStreamOperator<String> mapDsStream = connect.map(new CoMapFunction<Integer, String, String>() {
            @Override
            public String map1(Integer value) throws Exception {
                return String.valueOf(value);
            }

            @Override
            public String map2(String value) throws Exception {
                return value;
            }
        });
        mapDsStream.print();


        env.execute();
    }
}
