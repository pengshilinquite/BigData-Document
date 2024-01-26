import com.huawei.utils.{MyKafkaUtils, StreamingContextUtil}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.SparkConf
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.{HasOffsetRanges, KafkaUtils, OffsetRange}
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.{Seconds, StreamingContext}

/**
 * @author pengshilin
 * @date 2023/3/6 22:14
 */
object Test {
  def main(args: Array[String]): Unit = {
    val ssc: StreamingContext = StreamingContextUtil.getStreamingContext()
    val  ods_base_topic:String =  "ODS_BASE_LOG_1018"
    val group_id:String = "test-consumer-group"
    var offsetRange:Array[OffsetRange] =null

    val kafkaStream: InputDStream[ConsumerRecord[String, String]] = MyKafkaUtils.getKafkaDStream(ods_base_topic, ssc, group_id)
    val offsetDStream: DStream[ConsumerRecord[String, String]] = kafkaStream.transform(
      rdd => {
        offsetRange = rdd.asInstanceOf[HasOffsetRanges].offsetRanges
        rdd
      }
    )
    val record: DStream[String] = offsetDStream.map(_.value())
    record.print()

    ssc.start();
    ssc.awaitTermination();
  }
}
