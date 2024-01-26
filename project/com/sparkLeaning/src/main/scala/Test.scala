import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.dstream.ReceiverInputDStream
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.{DataFrame, Dataset, Encoders, Row, SparkSession}

object Test {
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("").setMaster("local")
    val sparkContext: SparkContext = new SparkContext(conf)
    val spark: SparkSession = SparkSession.builder().config(conf).getOrCreate()
    import spark.implicits._
    sparkContext.setLogLevel("warn")
    val value: RDD[(String, Int)] = sparkContext.makeRDD(List(("zhangsan", 20), ("wangwu", 10)))
    val ps: RDD[(String, Int)] = sparkContext.makeRDD(List(("zhangsan", 30), ("wangwu", 40)))
    val frame: DataFrame = value.toDF("username", "age")
    val ps2: DataFrame = ps.toDF("username", "age")
    frame.join(ps2,Seq("username","username"),"inner")
    frame.join(ps2,frame("username")===ps2("username"))



    val value1: RDD[(String, String)] = frame.rdd.map(row => {
      (row.getString(0), row.getString(1))
    })

    val value2: Dataset[(String, Int)] = frame.as[(String, Int)]

    val value3: Dataset[Person] = value.map(data => {
      Person(data._1, data._2)
    }).toDS()
    import org.apache.spark.sql.functions._
    value2.groupBy($"username",$"agg").agg(sum(lit(1))).as("column")


    val frame1: DataFrame = frame2

    val rdd1: RDD[Person] = value3.rdd
    val value4: RDD[(String, Int)] = rdd1.map(data => {
      (data.name, data.age)
    })

    val rdd: RDD[(String, Int)] = value2.rdd


  }

}
