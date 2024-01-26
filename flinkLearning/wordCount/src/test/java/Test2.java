import PoPj.Event;
import com.modle.UrlViewCount;
import com.waterMark.WindowLater2;
import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

import java.time.Duration;

/**
 * @author pengshilin
 * @date 2023/5/24 21:36
 */
public class Test2 {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        DataStreamSource<String> stringDataStreamSource = env.socketTextStream("192.168.149.10", 7777);
        SingleOutputStreamOperator<Event> mapStream = stringDataStreamSource.map(new MapFunction<String, Event>() {
            @Override
            public Event map(String data) throws Exception {
                String[] split = data.split(",");
                return new Event(split[0], split[1], Long.valueOf(split[2]));
            }
        });
        SingleOutputStreamOperator<Event> eventWaterMark = mapStream.assignTimestampsAndWatermarks(WatermarkStrategy.<Event>forBoundedOutOfOrderness(
                Duration.ofSeconds(1)
                ).withTimestampAssigner(new SerializableTimestampAssigner<Event>() {
                    @Override
                    public long extractTimestamp(Event event, long l) {
                        return event.timestamp;
                    }
                })
        );
        OutputTag<Event> outputTag = new OutputTag<Event>("later") {
        };
        SingleOutputStreamOperator<UrlViewCount> streamOperator = eventWaterMark.keyBy(data -> data.url)
                .window(TumblingEventTimeWindows.of(Time.seconds(3)))


                .allowedLateness(Time.seconds(0))
                .sideOutputLateData(outputTag)
                .aggregate(new WindowLater2.CountAgg(), new WindowLater2.CountResultProce());
        streamOperator.print("first");
        streamOperator.getSideOutput(outputTag).print("later");
        env.execute();
    }


    public static class CountAgg implements AggregateFunction<Event, Long, Long> {
        @Override
        public Long createAccumulator() {
            return 0L;
        }

        @Override
        public Long add(Event event, Long aLong) {
            return aLong + 1;
        }

        @Override
        public Long getResult(Long aLong) {
            return aLong;
        }

        @Override
        public Long merge(Long aLong, Long acc1) {
            return null;
        }
    }

    public static class CountResultProce extends ProcessWindowFunction<Long, UrlViewCount, String, TimeWindow> {
        @Override
        public void process(String url, Context context, Iterable<Long> iterable, Collector<UrlViewCount> collector) throws Exception {
            System.out.println(context.currentWatermark());
            long start = context.window().getStart();
            long end = context.window().getEnd();
            long count = iterable.iterator().next();
            collector.collect(new UrlViewCount(url+"水位綫:"+context.currentWatermark(), count, start, end));

        }
    }
}
