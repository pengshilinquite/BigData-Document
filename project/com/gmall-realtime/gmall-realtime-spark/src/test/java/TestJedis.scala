import com.huawei.utils.MyRedisUtils
import redis.clients.jedis.Jedis

/**
 * @author pengshilin
 * @date 2023/3/6 23:04
 */
object TestJedis {
  def main(args: Array[String]): Unit = {
    val jedis: Jedis = MyRedisUtils.getMyRedis()
    println(jedis.get("DIM_BASE_PROVINCE_2"))
    jedis.close()
  }

}
