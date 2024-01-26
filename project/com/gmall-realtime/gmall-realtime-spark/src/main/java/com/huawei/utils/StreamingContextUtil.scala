package com.huawei.utils


import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}

/**
 * @author pengshilin
 * @date 2023/3/5 12:07
 */
object StreamingContextUtil {

  def getStreamingContext():StreamingContext={
    val sparkConf: SparkConf = new SparkConf().setMaster("local[*]").setAppName("sparkLearning")
    val streamingContext: StreamingContext = new StreamingContext(sparkConf, Seconds(5))
    streamingContext
  }


}
