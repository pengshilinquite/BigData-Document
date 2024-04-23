package sink;


import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringEncoder;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.connector.source.util.ratelimit.RateLimiterStrategy;
import org.apache.flink.configuration.MemorySize;
import org.apache.flink.connector.datagen.source.DataGeneratorSource;
import org.apache.flink.connector.datagen.source.GeneratorFunction;
import org.apache.flink.connector.file.sink.FileSink;
import org.apache.flink.core.fs.Path;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.filesystem.OutputFileConfig;
import org.apache.flink.streaming.api.functions.sink.filesystem.bucketassigners.DateTimeBucketAssigner;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.DefaultRollingPolicy;
import org.apache.flink.streaming.api.functions.source.datagen.DataGenerator;

import java.time.Duration;
import java.time.ZoneId;

/**
 * @author pengshilin
 * @date 2024/4/10 15:01
 */
public class sinkFile {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(4);
        env.enableCheckpointing(2000, CheckpointingMode.EXACTLY_ONCE);
        DataGeneratorSource<String> generatorSource = new DataGeneratorSource<>(
                new GeneratorFunction<Long, String>() {
                    @Override
                    public String map(Long aLong) throws Exception {
                        return "a:" + aLong;
                    }
                }
                , 100
                , RateLimiterStrategy.perSecond(0.1)
                , Types.STRING
        );
        DataStreamSource<String> streamSource = env.fromSource(generatorSource, WatermarkStrategy.noWatermarks(), "自动生成source");
        //创建一个 FileSink
        FileSink<String> fileSink = FileSink
                //输出行式文件，指定编码
                .<String>forRowFormat(new Path("input/hdfs"), new SimpleStringEncoder<>("UTF-8"))
                //输出文件的一些配置，文件的前缀，后缀
                .withOutputFileConfig(
                        OutputFileConfig.builder()
                                .withPartPrefix("psl_")
                                .withPartSuffix(".log")
                                .build()


                )
                //按照目录进行分桶
                .withBucketAssigner(new DateTimeBucketAssigner<>("yyyy-MM-dd HH", ZoneId.systemDefault()))
                //滚动策略 10s 1M
                .withRollingPolicy(
                        DefaultRollingPolicy.builder()
                        .withRolloverInterval(Duration.ofSeconds(10))
                        .withMaxPartSize(new MemorySize(1024*1024))
                        .build()
                ).build();

        streamSource.sinkTo(fileSink);
        env.execute();
    }
}
