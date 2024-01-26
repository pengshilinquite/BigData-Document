package com.huawei.utils

import java.util.ResourceBundle

/**
 * @author pengshilin
 * @date 2023/3/5 11:53
 */
object PropertiesUtils {
  private val bundle: ResourceBundle = ResourceBundle.getBundle("config")

  def apply(key:String): String = {
    val str: String = bundle.getString(key)
    str
  }

  def main(args: Array[String]): Unit = {
    print(PropertiesUtils.apply("kafka.bootstrap.servers"))
  }

}
