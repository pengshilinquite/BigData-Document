package com.huawei.utils

import redis.clients.jedis.{Jedis, JedisPool, JedisPoolConfig}

/**
 * @author pengshilin
 * @date 2023/3/6 22:15
 */
object MyRedisUtils {
  var jedisPool:JedisPool = null
  def getMyRedis():Jedis ={
      if (jedisPool == null){
          var host:String = PropertiesUtils("redis.host")
          var port:String = PropertiesUtils("redis.port")
        val jedisPoolConfig = new JedisPoolConfig()
        jedisPoolConfig.setMaxTotal(100)//最大连接数10个
        jedisPoolConfig.setMaxIdle(50)//最大空闲5个
        jedisPoolConfig.setBlockWhenExhausted(true) //设置忙碌时长 毫秒
        jedisPoolConfig.setMaxWaitMillis(5000) //设置最大等待市场5000ms
        jedisPoolConfig.setTestOnBorrow(true)  //每次连接的进行测试
        jedisPool = new JedisPool(jedisPoolConfig,host,port.toInt)
      }
    jedisPool.getResource
  }
}
