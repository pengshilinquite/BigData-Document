package com.huawei.controller


import com.alibaba.fastjson.serializer.SerializeConfig
import com.alibaba.fastjson.{JSON, JSONArray, JSONObject}
import com.huawei.model.{PageActionLog, PageDisplayLog, PageLog, StartLog}
import com.huawei.utils.{MyKafkaUtils, MyRedisUtils, OffsetManagerUtil, StreamingContextUtil}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import org.apache.spark.streaming.kafka010.{HasOffsetRanges, KafkaUtils, OffsetRange}


/**
 * @author pengshilin
 * @date 2023/3/5 13:03
 */
object BaseLogApp {
  def main(args: Array[String]): Unit = {
    val ssc: StreamingContext = StreamingContextUtil.getStreamingContext()
    //原始主题
    val  ods_base_topic:String =  "ODS_BASE_LOG_1018"
    //启动主题
    val dwd_start_log:String =  "DWD_START_LOG_1018"
    //页面访问主题
    val dwd_page_log:String =  "DWD_PAGE_LOG_1018"
    //页面动作主题
    val dwd_page_action:String =  "DWD_PAGE_ACTION_1018"
    //页面曝光主题
    val dwd_page_display:String = "DWD_PAGE_DISPLAY_1018"
    //错误主题
    val dwd_error_info : String ="DWD_ERROR_INFO_1018"
    //消费组
    val group_id:String = "test-consumer-group"


    val topicOffset: Map[TopicPartition, Long] = OffsetManagerUtil.getOffset(ods_base_topic, group_id)
    var kafkaStream: InputDStream[ConsumerRecord[String, String]] =null
    if(topicOffset!=null&&topicOffset.nonEmpty){
      kafkaStream = MyKafkaUtils.getKafkaDStream(ods_base_topic, ssc, group_id,topicOffset)
    }else{
      kafkaStream = MyKafkaUtils.getKafkaDStream(ods_base_topic, ssc, group_id)
    }

    //transform  转换算子，不做任何操作，返回还是rdd
    //提取kafka的offset
    var offsetRange:Array[OffsetRange] =null
    val offsetDStream: DStream[ConsumerRecord[String, String]] = kafkaStream.transform(
      rdd => {
        offsetRange = rdd.asInstanceOf[HasOffsetRanges].offsetRanges
        rdd
      }
    )
    val jsonObjectStream2: DStream[JSONObject] = offsetDStream.map(
      record => {
        val value: String = record.value()
        val jSONObject: JSONObject = JSON.parseObject(value)
        jSONObject
      }
    )
    jsonObjectStream2.print()

    val jsonObjectStream: DStream[JSONObject] = offsetDStream.map(
      record => {
        val value: String = record.value()
        val jSONObject: JSONObject = JSON.parseObject(value)
        jSONObject
      }
    )
    jsonObjectStream.print(100)

    jsonObjectStream.foreachRDD(
      rdd=>{
        rdd.foreachPartition(
          jsonObItem=>{
            for (data <- jsonObItem) {
              val errJson: JSONObject = data.getJSONObject("err")

              if(errJson!=null){
                MyKafkaUtils.send(dwd_error_info,JSON.toJSONString(errJson,new SerializeConfig(true)))
              }else {

                val commonJson: JSONObject = data.getJSONObject("common")
                val mid: String = commonJson.getString("mid")
                val uid: String = commonJson.getString("uid")
                val ar: String = commonJson.getString("ar")
                val ch: String = commonJson.getString("ch")
                val os: String = commonJson.getString("os")
                val md: String = commonJson.getString("md")
                val vc: String = commonJson.getString("vc")
                val isNew: String = commonJson.getString("is_new")
                val ts: Long = data.getLong("ts")
                val pageJson: JSONObject = data.getJSONObject("page")
                if (pageJson !=null){
                  val pageId: String = pageJson.getString("page_id")
                  val pageItem: String = pageJson.getString("item")
                  val pageItemType: String = pageJson.getString("item_type")
                  val lastPageId: String = pageJson.getString("last_page_id")
                  val duringTime: Long = pageJson.getLong("during_time")
                  val pageLog =PageLog(mid, uid, ar, ch, isNew, md, os, vc, pageId,lastPageId, pageItem, pageItemType, duringTime, ts)
                  MyKafkaUtils.send(dwd_page_log, JSON.toJSONString(pageLog, new SerializeConfig(true)))
                  val actionArray: JSONArray = data.getJSONArray("actions")
                  if (actionArray != null && actionArray.size() > 0) {
                    for (i <- 0 until actionArray.size()) {
                      val actionObject: JSONObject = actionArray.getJSONObject(i)
                      val actionId: String = actionObject.getString("action_id")
                      val actionItem: String = actionObject.getString("item")
                      val actionItemType: String = actionObject.getString("item_type")

                      val actionTs: Long = actionObject.getLong("ts")
                      val pageActionLog = PageActionLog(mid, uid, ar, ch, isNew, md, os, vc, pageId, lastPageId, pageItem, pageItemType, duringTime, actionId, actionItem, actionItemType, actionTs)
                      MyKafkaUtils.send(dwd_page_action, JSON.toJSONString(pageActionLog, new SerializeConfig(true)))

                    }
                    val displaysArray: JSONArray = data.getJSONArray("displays")
                    if (displaysArray != null && displaysArray.size() > 0) {
                      for (i <- 0 until displaysArray.size()) {
                        val displayObject: JSONObject = displaysArray.getJSONObject(i)
                        val displayType: String = displayObject.getString("display_type")
                        val displayItem: String = displayObject.getString("item")
                        val displayItemType: String = displayObject.getString("item_type")
                        val displayOrder: String = displayObject.getString("order")
                        val displayPosId: String = displayObject.getString("pos_id")
                        val displayLog = PageDisplayLog(mid, uid, ar, ch, isNew, md, os, vc, pageId, lastPageId, pageItem, pageItemType, duringTime, displayType, displayItem, displayItemType, displayOrder, displayPosId, ts)
                        MyKafkaUtils.send(dwd_page_display, JSON.toJSONString(displayLog, new SerializeConfig(true)))
                      }

                    }
                  }

                }

                val startObject: JSONObject = data.getJSONObject("start")
                if (startObject != null) {
                  val entry: String = startObject.getString("entry")
                  val loadingTimeMs: Long = startObject.getLong("loading_time_ms")
                  val openAdId: String = startObject.getString("open_ad_id")
                  val openAdMs: Long = startObject.getLong("open_ad_ms")
                  val openAdSkipMs: Long = startObject.getLong("open_ad_skip_ms")
                  //封装 Bean
                  val startLog = StartLog(mid, uid, ar, ch, isNew, md, os, vc, entry, openAdId, loadingTimeMs, openAdMs, openAdSkipMs, ts)
                  //发送 Kafka

                  MyKafkaUtils.send(dwd_start_log, JSON.toJSONString(startLog, new SerializeConfig(true)))
                }
              }
            }
            MyKafkaUtils.flush()
          }
        )
        OffsetManagerUtil.saveOffset(ods_base_topic,group_id,offsetRange)
      }
    )

    ssc.start()
    ssc.awaitTermination()

  }

}
