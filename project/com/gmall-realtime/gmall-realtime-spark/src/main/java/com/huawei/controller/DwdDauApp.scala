package com.huawei.controller


import java.lang
import java.text.SimpleDateFormat
import java.time.{LocalDate, Period}
import java.util.Date

import com.alibaba.fastjson.{JSON, JSONObject}
import com.huawei.model.{DauInfo, PageLog}
import com.huawei.utils.{MyBeanUtils, MyKafkaUtils, MyRedisUtils, OffsetManagerUtil, StreamingContextUtil}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import org.apache.spark.streaming.kafka010.{HasOffsetRanges, OffsetRange}
import redis.clients.jedis.Jedis

import scala.collection.mutable.ListBuffer


/**
 * @author pengshilin
 * @date 2023/3/9 21:00
 */
object DwdDauApp {

  /**
   * 1.准备实时环境
   * 2.从redis中读取偏移量
   * 3.从kafka中消费数据
   * 4.读取offset
   * 5.处理数据
   *      5.1 转换数据结构
   *      5.2 对数据进行去重
   *      5.3维度关联
   * 6.写入Es
   * 7.提交offset
   */
  def main(args: Array[String]): Unit = {
    //创建连接环境
    val ssc: StreamingContext = StreamingContextUtil.getStreamingContext()
    //声明topic和groupId
    val topicName:String = "DWD_PAGE_LOG_1018"
    val groupId:String = "test-consumer-group"
    //读取Offset
    val offset: Map[TopicPartition, Long] = OffsetManagerUtil.getOffset(topicName, groupId)
    //从kafka中消费数据
    var kafkaDStream: InputDStream[ConsumerRecord[String, String]] = null
    if (offset !=null && offset.nonEmpty){
      kafkaDStream = MyKafkaUtils.getKafkaDStream(topicName, ssc, groupId, offset)
    }else{
      kafkaDStream= MyKafkaUtils.getKafkaDStream(topicName,ssc,groupId)
    }
    //从KakfaDstream 中获得偏移量
    var offsetRanges: Array[OffsetRange] =null
    val offsetDStream: DStream[ConsumerRecord[String, String]] = kafkaDStream.transform(
      rdd => {
        offsetRanges = rdd.asInstanceOf[HasOffsetRanges].offsetRanges
        rdd
      }
    )
    //进行数据结构的转换
    val pageLogDStream: DStream[PageLog] = offsetDStream.map(
      consumer => {
        val value: String = consumer.value()
        val pageLog: PageLog = JSON.parseObject(value, classOf[PageLog])
        pageLog

      }
    )


    val filterDStream: DStream[PageLog] = pageLogDStream.filter(
      PageLog => {
        PageLog.last_page_id == null && PageLog.ts !=0

      }
    )
   // filterDStream.print(100)


    val redisFilterDStream: DStream[PageLog] = filterDStream.mapPartitions(
      pageLogIter => {
        val jedis: Jedis = MyRedisUtils.getMyRedis()

        val pageLogs: ListBuffer[PageLog] = ListBuffer[PageLog]()
        val pageLogList: List[PageLog] = pageLogIter.toList
        val simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd")
        println("第三方审查前" + pageLogList.size)
        for (pageLog <- pageLogList) {
          val mid: String = pageLog.mid
          val ts: Long = pageLog.ts
          val date = new Date(ts)
          val dateFormat: String = simpleDateFormat.format(date)

          val redisDuKey: String = s"DUA:$dateFormat"
          val isNew: lang.Long = jedis.sadd(redisDuKey, mid)
          if (isNew == 1L) {
            pageLogs.append(pageLog)
          }


        }


        jedis.close()
        println("第三方审查后:"+pageLogs.size)
        pageLogs.iterator
      }
    )


    val dauInfoDStream: DStream[DauInfo] = redisFilterDStream.mapPartitions(
      pageLogIter => {
        val dauInfos: ListBuffer[DauInfo] = ListBuffer[DauInfo]()
        val sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val jedis: Jedis = MyRedisUtils.getMyRedis()
        for (pageLog <- pageLogIter) {
          val dauInfo: DauInfo = new DauInfo()
          MyBeanUtils.copyProperties(pageLog, dauInfo)

          val uid: String = pageLog.user_id
          val redisUidkey: String = s"DIM_USER_INFO_$uid"
          val userInfoJson: String = jedis.get(redisUidkey)
          val userInfoObject: JSONObject = JSON.parseObject(userInfoJson)
          //提取性别和年龄
          val gender: String = userInfoObject.getString("gender")
          val birthday: String = userInfoObject.getString("birthday")
          val birthdayId: LocalDate = LocalDate.parse(birthday)
          val nowId: LocalDate = LocalDate.now()
          val period: Period = Period.between(birthdayId, nowId)
          val age: Int = period.getYears
          dauInfo.user_gender = gender
          dauInfo.user_age = age.toString

          val province_id: String = dauInfo.province_id
          val provinceJson: String = jedis.get(s"DIM_BASE_PROVINCE_$province_id")
          val provinceJsonObj: JSONObject = JSON.parseObject(provinceJson)
          val proviceName: String = provinceJsonObj.getString("name")
          val provinceIsoCode: String = provinceJsonObj.getString("iso_code")
          val provinceIso3166: String = provinceJsonObj.getString("iso_3166_2")
          val provinceAreaCode: String = provinceJsonObj.getString("area_code")
          dauInfo.province_name = proviceName
          dauInfo.province_iso_code = provinceIsoCode
          dauInfo.province_3166_2 = provinceIso3166
          dauInfo.province_area_code = provinceAreaCode
          val date = new Date(pageLog.ts)
          val dtHr: String = sdf.format(date)
          val dtHrArr: Array[String] = dtHr.split(" ")
          val dt: String = dtHrArr(0)
          val hr: String = dtHrArr(1).split(":")(0)
          dauInfo.dt = dt
          dauInfo.hr = hr
          dauInfos.append(dauInfo)
        }
        jedis.close()
        dauInfos.iterator
      }
    )

    //写入OLAP中
    dauInfoDStream.print(100)






    ssc.start()
    ssc.awaitTermination()
  }

}
