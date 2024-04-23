package transform.combine;

import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.streaming.api.datastream.ConnectedStreams;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.co.CoProcessFunction;
import org.apache.flink.util.Collector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author pengshilin
 * @date 2024/4/9 17:15
 */
public class ConnectByKeyDemo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        DataStreamSource<Tuple2<Integer, String>> source1 = env.fromElements(
                Tuple2.of(1, "a1"),
                Tuple2.of(1, "a2"),
                Tuple2.of(2, "b"),
                Tuple2.of(3, "c")
        );
        DataStreamSource<Tuple3<Integer, String,Integer>> source2 = env.fromElements(
                Tuple3.of(1, "aa1",1),
                Tuple3.of(1, "aa2",2),
                Tuple3.of(2, "bb",1),
                Tuple3.of(3, "cc",1)
        );
        ConnectedStreams<Tuple2<Integer, String>, Tuple3<Integer, String, Integer>> connectedStreams = source1.connect(source2);
        // 重点： 多并行度下，需要根据 关联条件 进行keyby，才能保证key相同的数据到一起去，才能匹配上
        ConnectedStreams<Tuple2<Integer, String>, Tuple3<Integer, String, Integer>> connectKey = connectedStreams.keyBy(s1 -> s1.f0, s2 -> s2.f0);

        /**
         * 实现交互匹配结果，两条流的数据不一定谁先来
         * 1、每条流，有数据，先存到一个变量中
         *          hashMap
         *          key =》 id
         *          value = 》 list<数据>
         * 2、每条流有数据来的时候，除了存变量中，不知道对方是否有匹配的数据，要去另外一条流存的变量中 查找是否有匹配上的
         */



        SingleOutputStreamOperator<String> process = connectKey.process(new CoProcessFunction<Tuple2<Integer, String>, Tuple3<Integer, String, Integer>, String>() {

            //每条流定义一个hashMap，用来存数据
            Map<Integer, List<Tuple2<Integer, String>>> s1cache = new HashMap<Integer, List<Tuple2<Integer, String>>>();
            Map<Integer, List<Tuple3<Integer, String, Integer>>> s2cache = new HashMap<Integer, List<Tuple3<Integer, String, Integer>>>();

            @Override
            public void processElement1(Tuple2<Integer, String> value, Context ctx, Collector<String> out) throws Exception {
                Integer id = value.f0;
                if (!s1cache.containsKey(id)) {
                    //1.1如果key 不存在，说明该key 是第一条数据，初始化，put放进map中
                    List<Tuple2<Integer, String>> s1value = new ArrayList<>();
                    s1value.add(value);
                    s1cache.put(id, s1value);

                } else {
                    s1cache.get(id).add(value);
                }
                //根据id，查找s2的数据，只输出 匹配上 的数据
                if (s2cache.containsKey(id)) {
                    for (Tuple3<Integer, String, Integer> integerTuple3 : s2cache.get(id)) {
                        out.collect("s1:" + value + "=====" + "s2:" + integerTuple3);
                    }

                }
            }

            @Override
            public void processElement2(Tuple3<Integer, String, Integer> value, Context ctx, Collector<String> out) throws Exception {
                Integer id = value.f0;
                if (!s2cache.containsKey(id)) {
                    //1.1如果key 不存在，说明该key 是第一条数据，初始化，put放进map中
                    List<Tuple3<Integer, String, Integer>> s2value = new ArrayList<>();
                    s2value.add(value);
                    s2cache.put(id, s2value);

                } else {
                    s2cache.get(id).add(value);
                }
                if (s1cache.containsKey(id)) {
                    for (Tuple2<Integer, String> integerTuple2 : s1cache.get(id)) {
                        out.collect("s2:" + value + "=====" + "s1:" + integerTuple2);
                    }

                }


            }
        });
        process.print();


        env.execute();
    }
}
