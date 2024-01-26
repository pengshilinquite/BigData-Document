package com.huawei.utils

import java.util.Properties

import org.apache.kafka.clients.consumer
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import org.apache.kafka.common.TopicPartition
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, KafkaUtils, LocationStrategies}

import scala.collection.mutable

/**
 * @author pengshilin
 * @date 2023/3/5 11:59
 */
object MyKafkaUtils {


  private val kafkaPara: mutable.Map[String, String] = mutable.Map(
    ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG -> PropertiesUtils.apply("kafka.bootstrap.servers"),
    ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG ->
      "org.apache.kafka.common.serialization.StringDeserializer",
    ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG ->
      "org.apache.kafka.common.serialization.StringDeserializer",
    ConsumerConfig.GROUP_ID_CONFIG -> "gmall",
    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG -> "latest",
    ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG -> "true"
  )
  /**
   * 连接kafka，需要传入topic ,StreamingContext,groupid
   * @param topic
   */
  def getKafkaDStream(topic:String,ssc: StreamingContext,groupId:String): InputDStream[ConsumerRecord[String, String]]={
    kafkaPara(ConsumerConfig.GROUP_ID_CONFIG) = groupId;
    println(kafkaPara(ConsumerConfig.GROUP_ID_CONFIG))
    val dsStream: InputDStream[ConsumerRecord[String, String]] = KafkaUtils.createDirectStream(ssc,
      LocationStrategies.PreferConsistent,
      ConsumerStrategies.Subscribe[String, String](Array(topic), kafkaPara))
    dsStream
  }
  def getKafkaDStream(topic:String,ssc: StreamingContext,groupId:String,offset:Map[TopicPartition,Long]): InputDStream[ConsumerRecord[String, String]]={
    kafkaPara(ConsumerConfig.GROUP_ID_CONFIG) = groupId;
    println(kafkaPara(ConsumerConfig.GROUP_ID_CONFIG))
    val dsStream: InputDStream[ConsumerRecord[String, String]] = KafkaUtils.createDirectStream(ssc,
      LocationStrategies.PreferConsistent,
      ConsumerStrategies.Subscribe[String, String](Array(topic), kafkaPara,offset))
    dsStream
  }

  def createKafkaProducer():KafkaProducer[String,String] = {
    val props = new Properties()
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,PropertiesUtils("kafka.bootstrap.servers"))
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringSerializer")
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringSerializer")
    props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,"true")
    props.put(ProducerConfig.ACKS_CONFIG,"all")
    val producer:KafkaProducer[String, String] = new KafkaProducer[String, String](props)
    producer
  }

  def send(topic:String,msg:String)={
    createKafkaProducer.send(new ProducerRecord(topic,msg))
  }
  def send(topic:String,msg:String,key:String)={
    createKafkaProducer.send(new ProducerRecord(topic,key,msg))
  }

  def flush():Unit={
    if(createKafkaProducer!=null){
      createKafkaProducer.flush()
    }
  }
  def close(): Unit ={
    if (createKafkaProducer != null){
      createKafkaProducer.close()
    }

    def main(args: Array[String]): Unit = {
      println(createKafkaProducer)
    }
  }



}
