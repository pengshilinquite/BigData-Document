package com.huawei.controller

import java.time.{LocalDate, Period}

import com.alibaba.fastjson.{JSON, JSONObject}
import com.huawei.model.{OrderDetail, OrderInfo, OrderWide}
import com.huawei.utils.{MyKafkaUtils, MyRedisUtils, OffsetManagerUtil}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.spark.SparkConf
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import org.apache.spark.streaming.kafka010.{HasOffsetRanges, OffsetRange}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import redis.clients.jedis.Jedis

import scala.collection.mutable.ListBuffer

/**
 * @author pengshilin
 * @date 2023/3/11 17:54
 */
object DwdOrderApp {
  def main(args: Array[String]): Unit = {


  //创建连接环境
   val conf: SparkConf = new SparkConf().setMaster("local").setAppName("dwd_order_app")
   val ssc: StreamingContext = new StreamingContext(conf,Seconds(5))
  //获取偏移量
  val orderInfoTopicName:String="DWD_ORDER_INFO_I"
  val orderInfoGroup:String = "test-consumer-group"
   val orderInfoOffsets: Map[TopicPartition, Long] = OffsetManagerUtil.getOffset(orderInfoTopicName, orderInfoGroup)

  val orderDetailTopicName:String = "DWD_ORDER_DETAIL_I"
  val orderDetailGroup:String = "test-consumer-group"
   val orderDetailOffsets: Map[TopicPartition, Long] = OffsetManagerUtil.getOffset(orderDetailTopicName, orderDetailGroup)


  //从kafka中消费数据
  //order_info
  var orderInfoKafkaDStream: InputDStream[ConsumerRecord[String, String]] =null
  if (orderInfoOffsets!=null && orderInfoOffsets.nonEmpty){
    orderInfoKafkaDStream = MyKafkaUtils.getKafkaDStream(orderInfoTopicName, ssc, orderInfoGroup, orderInfoOffsets)
  }else{
    orderInfoKafkaDStream = MyKafkaUtils.getKafkaDStream(orderInfoTopicName, ssc, orderInfoGroup)
  }

  var orderDetailKafkaDStream: InputDStream[ConsumerRecord[String, String]] =null
  if (orderDetailOffsets!=null && orderDetailOffsets.nonEmpty){
    orderDetailKafkaDStream = MyKafkaUtils.getKafkaDStream(orderDetailTopicName, ssc, orderDetailGroup, orderDetailOffsets)
  }else{
    orderDetailKafkaDStream = MyKafkaUtils.getKafkaDStream(orderDetailTopicName, ssc, orderDetailGroup)
  }

  //提取offset
  var orderInfoOffsetRanges: Array[OffsetRange] =null
   val orderInfoOffsetsDStream: DStream[ConsumerRecord[String, String]] = orderInfoKafkaDStream.transform(
    rdd => {
      orderInfoOffsetRanges = rdd.asInstanceOf[HasOffsetRanges].offsetRanges
      rdd
    }
  )
  var orderDetailOffsetRanges: Array[OffsetRange] =null
   val orderDetailOffsetDStream: DStream[ConsumerRecord[String, String]] = orderDetailKafkaDStream.transform(
    rdd => {
      orderDetailOffsetRanges = rdd.asInstanceOf[HasOffsetRanges].offsetRanges
      rdd
    }
  )

   val orderInfoDStream: DStream[OrderInfo] = orderInfoOffsetsDStream.map(
    consumerRecord => {
      val value: String = consumerRecord.value()
      val orderInfo: OrderInfo = JSON.parseObject(value, classOf[OrderInfo])
      orderInfo
    }
  )
    //
    // orderInfoDStream.print(100)

   val orderDetailDStream: DStream[OrderDetail] = orderDetailOffsetDStream.map(
    consumerRecord => {
      val value: String = consumerRecord.value()
      val orderDetail: OrderDetail = JSON.parseObject(value,classOf[OrderDetail])
      orderDetail
    }
  )
    val orderInfoDimDStream: DStream[OrderInfo] = orderInfoDStream.mapPartitions(
      orderInfoIters => {
        //val orderInfos: ListBuffer[OrderInfo] = ListBuffer[OrderInfo]()
        val orderInfos: List[OrderInfo] = orderInfoIters.toList
        val jedis: Jedis = MyRedisUtils.getMyRedis()
        for (orderInfo <- orderInfos) {
          val uid: Long = orderInfo.user_id
          val redisUserKey: String = s"DIM_USER_INFO_$uid"
          val userInfoJson: String = jedis.get(redisUserKey)
          val userInfoJsonObj: JSONObject = JSON.parseObject(userInfoJson)
          val gender: String = userInfoJsonObj.getString("gender")
          val birthday: String = userInfoJsonObj.getString("birthday")
          val birthdayId: LocalDate = LocalDate.parse(birthday)
          val nowId: LocalDate = LocalDate.now()
          val period: Period = Period.between(birthdayId, nowId)
          val age: Int = period.getYears
          orderInfo.user_gender = gender
          orderInfo.user_age = age

          //管理地区维度
          val province_id: Long = orderInfo.province_id
          val provinceJson:String = jedis.get(s"DIM_BASE_PROVINCE_$province_id")
          val provinceJsonObj: JSONObject = JSON.parseObject(provinceJson)
          try {
            val provinceName: String = provinceJsonObj.getString("name")
            val provinceAreaCode: String = provinceJsonObj.getString("area_code")
            val provinceIso31662: String = provinceJsonObj.getString("iso_3166_2")
            val provinceIsoCode: String = provinceJsonObj.getString("iso_code")
            orderInfo.province_name = provinceName
            orderInfo.province_area_code = provinceAreaCode
            orderInfo.province_3166_2_code = provinceIso31662
            orderInfo.province_iso_code = provinceIsoCode

            //处理日期
            val createTime: String = orderInfo.create_time
            val createDtHr: Array[String] = createTime.split(" ")
            val createDate: String = createDtHr(0)
            val createHr: String = createDtHr(1).split(":")(0)
            orderInfo.create_date = createDate
            orderInfo.create_hour = createHr
          } catch {
            case exception: Exception => println(provinceJsonObj)
          }
//         //orderInfos.append(orderInfo)
        }
        jedis.close()
        orderInfos.iterator
      }
    )
    val orderInfoKVDStream: DStream[(Long, OrderInfo)] = orderInfoDimDStream.map(
      orderInfo => (orderInfo.id, orderInfo)
    )
    val orderDetailKVDStream: DStream[(Long, OrderDetail)] = orderDetailDStream.map(
      orderDetail => (orderDetail.order_id, orderDetail)
    )
    val orderJoinDStream: DStream[(Long, (Option[OrderInfo], Option[OrderDetail]))] = orderInfoKVDStream.fullOuterJoin(orderDetailKVDStream)
    orderJoinDStream.mapPartitions(
      orderJoinIter=>{
        val orderWides: ListBuffer[OrderWide] = ListBuffer[OrderWide]()
        for ((key,(orderInfoOP,orderDetailOp)) <- orderJoinIter) {
                if(orderInfoOP.isDefined){
                  val orderInfo: OrderInfo = orderInfoOP.get
                  if(orderDetailOp.isDefined){
                    val orderDetail: OrderDetail = orderDetailOp.get
                    val orderWide: OrderWide = new OrderWide(orderInfo, orderDetail)
                    orderWides.append(orderWide)
                  } else{

                  }
                }
        }
        orderWides.iterator
      }

    )

  ssc.start()
  ssc.awaitTermination()

  }
}
