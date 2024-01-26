import PoPj.Event;
import com.function.MyStudent;
import com.modle.Student;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.SourceFunction;

import java.util.Random;

/**
 * @author pengshilin
 * @date 2023/4/9 15:35
 */
public class FlinkBase {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        DataStreamSource<Event> eventDataStreamSource = env.addSource(new SourceFunction<Event>() {
            String [] users = {"Mary"};
            String [] urls = {"./home"};
            int [] num = {1,2,3,4,5,6,7,8,9};
            Random random = new Random();
            boolean run = true;
            @Override
            public void run(SourceContext<Event> sourceContext) throws Exception {
                int i = 0;
                while (i<num.length){
                    sourceContext.collect(new Event(
                            users[random.nextInt(users.length)],
                            urls[random.nextInt(urls.length)],
                            10000l-num[i]*1000


                    ));
                    i++;
                    Thread.sleep(1000);
                }
            }

            @Override
            public void cancel() {
                run = false;
            }
        });
        eventDataStreamSource.print();

        env.execute();

    }
}
