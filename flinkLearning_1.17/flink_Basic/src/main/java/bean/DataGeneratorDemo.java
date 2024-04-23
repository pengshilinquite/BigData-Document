package bean;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.connector.source.util.ratelimit.RateLimiterStrategy;
import org.apache.flink.connector.datagen.source.DataGeneratorSource;
import org.apache.flink.connector.datagen.source.GeneratorFunction;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * @author pengshilin
 * @date 2024/4/8 14:51
 * 数据生成器
 */
public class DataGeneratorDemo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);


        /**
         * 数据生成器
         * new GeneratorFunction<Long, String>()     Long 是自动生成的数字(固定)，返回一个string 类型
         * 10    --生成的条数   //  Long.MAX_VALUE-- 无界流
         * RateLimiterStrategy.perSecond(2)   --每秒钟生成2条数据
         *Types.STRING   --返回类型为String
         */

        DataGeneratorSource<String> generatorSource = new DataGeneratorSource<>(
                new GeneratorFunction<Long, String>() {
                    @Override
                    public String map(Long aLong) throws Exception {
                        Date currentDate = new Date();
                        SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        String currentTime = timeFormat.format(currentDate);
                        return "number: " + aLong+ "  time: "+ currentTime;
                    }
                }
                , 10
                , RateLimiterStrategy.perSecond(2)
                , Types.STRING
        );

        DataStreamSource<String> generaStream = env.fromSource(generatorSource, WatermarkStrategy.noWatermarks(), "数据生成器");
        generaStream.print();

        env.execute();
    }
}
