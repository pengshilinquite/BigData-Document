import com.alibaba.fastjson.{JSON, JSONObject}
import com.huawei.utils.{MyKafkaUtils, StreamingContextUtil}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.{DStream, InputDStream}

/**
 * @author pengshilin
 * @date 2023/3/6 21:58
 */
object Test2 {
  def main(args: Array[String]): Unit = {
    val sc: StreamingContext = StreamingContextUtil.getStreamingContext()
    val topic_name:String="clicks"
    val group_id_name:String="test-consumer-group"
    val record: InputDStream[ConsumerRecord[String, String]] = MyKafkaUtils.getKafkaDStream(topic_name, sc, group_id_name)
    val jsonObjectDStream: DStream[JSONObject] = record.map(
      record => {
        val logData: String = record.value()
        val jSONObject: JSONObject = JSON.parseObject(logData)
        jSONObject
      }
    )
    jsonObjectDStream.print(100)
    sc.start()
    sc.awaitTermination()
  }
}
