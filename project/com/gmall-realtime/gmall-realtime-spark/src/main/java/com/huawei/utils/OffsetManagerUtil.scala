package com.huawei.utils

import java.util

import org.apache.kafka.common.TopicPartition
import org.apache.spark.streaming.kafka010.OffsetRange
import redis.clients.jedis.Jedis

import scala.collection.mutable

/**
 * @author pengshilin
 * @date 2023/3/6 23:46
 */
object OffsetManagerUtil {
  def getOffset(topicName:String,groupId:String):Map[TopicPartition, Long]={
    val jedis: Jedis = MyRedisUtils.getMyRedis()
    val offsetKey:String = "offsets:"+ topicName+":"+groupId
    val offsetMap: util.Map[String, String] = jedis.hgetAll(offsetKey)
   // println(jedis.keys("*"))
    println("读取到key:"+offsetKey+"-----"+"读取到offset"+offsetMap)

    val result: mutable.Map[TopicPartition, Long] = mutable.Map[TopicPartition, Long]()
    import scala.collection.JavaConverters._
    for ((partition,offset) <- offsetMap.asScala) {
      val topicPartition = new TopicPartition(topicName, partition.toInt)
       result.put(topicPartition,offset.toLong)
    }
    jedis.close()
    result.toMap
  }

  def saveOffset(topicName:String,groupId:String,offsetRanges:Array[OffsetRange])={
    val jedis: Jedis = MyRedisUtils.getMyRedis()
    val offsetKey:String = s"offsets:$topicName:$groupId"
    val offsetMap: util.HashMap[String, String] = new util.HashMap[String, String]()
    if(offsetRanges.length>0&&offsetRanges!=null){
      for (offsetRange <- offsetRanges) {
        val partition: Int = offsetRange.partition
        val endOffset: Long = offsetRange.untilOffset
        offsetMap.put(partition.toString,endOffset.toString)
      }
    }
    println("提交offset:"+offsetMap)
    jedis.hset(offsetKey,offsetMap)
  }

}
