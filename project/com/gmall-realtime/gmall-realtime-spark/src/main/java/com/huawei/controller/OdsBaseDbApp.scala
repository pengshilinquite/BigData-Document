package com.huawei.controller

import java.util

import com.alibaba.fastjson.serializer.SerializeConfig
import com.alibaba.fastjson.{JSON, JSONObject}
import com.huawei.utils
import com.huawei.utils.{MyKafkaUtils, MyRedisUtils, OffsetManagerUtil, StreamingContextUtil}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import org.apache.spark.streaming.kafka010.{HasOffsetRanges, OffsetRange}
import redis.clients.jedis.Jedis

/**
 * @author pengshilin
 * @date 2023/3/8 0:03
 */

/**
 * 业务数据消费
 *    1.连接StreamingContext
 *    2.消费数据：传入topic 和groupId 和 ssc +offset（可以选择redis存储）
 */
object OdsBaseDbApp {
  def main(args: Array[String]): Unit = {
    //建立环境信息，订阅消费kafka
    val ssc: StreamingContext = StreamingContextUtil.getStreamingContext()
    val topicName:String ="ODS_BASE_DB_M"
    val groupId:String = "test-consumer-group"
    //获取offset，如果没有默认选择kafka里面的offset
    val topicOffset: Map[TopicPartition, Long] = OffsetManagerUtil.getOffset(topicName, groupId)
    var kafkaStream: InputDStream[ConsumerRecord[String, String]]=null
    if (topicOffset!=null&&topicOffset.nonEmpty){
      kafkaStream= MyKafkaUtils.getKafkaDStream(topicName, ssc, groupId, topicOffset)
    }else{
      kafkaStream= MyKafkaUtils.getKafkaDStream(topicName, ssc, groupId)
    }

    //提取KafkaStream 的offset ，并在写入kafka后，提交offset 到redis中
    //使用转换算子 transform ,不改动rdd的结构
    var offsetRanges: Array[OffsetRange]=null
    val offsetDStream: DStream[ConsumerRecord[String, String]] = kafkaStream.transform(
      rdd => {
        offsetRanges = rdd.asInstanceOf[HasOffsetRanges].offsetRanges
        rdd
      }
    )

    //提取kafka的数据value
    val jsonObjectDStream: DStream[JSONObject] = offsetDStream.map(
      rdd => {
        val value: String = rdd.value()
        JSON.parseObject(value)
      }
    )
    //打印看是否有数据
    //jsonObjectDStream.print(100)

    //事实表清单
    //val factTable:Array[String] =  Array[String]("order_info","order_detail")
    //val dimTable:Array[String] = Array[String]("user_info","base_province")




    //对数据进行数据转换

    jsonObjectDStream.foreachRDD(


      rdd=>{

        //动态表清单  一个批次的数据读取一次redis
        val redisFactKeys:String = "Fact:Tables"
        val redisDimkeys:String = "DIM:Tables"
        val jedisTable: Jedis = MyRedisUtils.getMyRedis()
        val factTable: util.Set[String] = jedisTable.smembers(redisFactKeys)
        println(factTable)
        val factTableBC: Broadcast[util.Set[String]] = ssc.sparkContext.broadcast(factTable)
        val dimTable: util.Set[String] = jedisTable.smembers(redisDimkeys)
        println(dimTable)
        val dimTableBC: Broadcast[util.Set[String]] = ssc.sparkContext.broadcast(dimTable)
        jedisTable.close()



        rdd.foreachPartition(

          iter=>{
            val jedis: Jedis = MyRedisUtils.getMyRedis()
            for (jsonObject <- iter) {
              val operType: String = jsonObject.getString("type")
              val opValue: String = operType match {
                case "insert" => "I"
                case "update" => "U"
                case "delete" => "D"
                case "bootstrap-insert" =>"I"
                case _ => null
              }
              if (opValue!=null){
                //提取表名
                val tableName: String = jsonObject.getString("table")
                val data: JSONObject = jsonObject.getJSONObject("data")

                //事实数据：
                if(factTableBC.value.contains(tableName)){
                  val dwdTopicName:String=s"DWD_${tableName.toUpperCase()}_$opValue"
                  println(dwdTopicName)
                  MyKafkaUtils.send(dwdTopicName,JSON.toJSONString(data,new SerializeConfig(true)))
                }

                //维度数据
                if(dimTableBC.value.contains(tableName)){
                  val id: String = data.getString("id")
                  val redisKey:String =s"DIM_${tableName.toUpperCase()}_$id"
                  println(redisKey+"============================"+data.toJSONString)
                  jedis.set(redisKey,data.toJSONString)

                }
              }
            }
            jedis.close()
            MyKafkaUtils.flush()

          }

        )

        OffsetManagerUtil.saveOffset(topicName,groupId,offsetRanges)

      }

    )





    ssc.start()
    ssc.awaitTermination()
  }
}
