package com.psl;

import org.apache.hadoop.hive.ql.exec.UDF;


import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * @author pengshilin
 * @date 2023/12/22 0:28
 * 将字符串加减乘除运算计算为数字并返回
 * create function calculate as 'com.psl.CalculateString' using jar 'hdfs:/user/warehouse/hive-udf-1.0-SNAPSHOT.jar';
 * select calculate("478*0.34738+1")
 */

public class CalculateString extends UDF {
    public String evaluate(String input) throws ScriptException {

        if (input ==null || input.length()==0){
            return null;
        } else {
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine se = manager.getEngineByName("js");
            String result = String.valueOf(se.eval(input)) ;

            return result;
        }
    }

    public static void main(String[] args) throws ScriptException {
        CalculateString calculateString = new CalculateString();
        System.out.println(calculateString.evaluate("4.34297*349"));
        //System.out.println(calculateString.evaluate(""));
        //System.out.println(calculateString.evaluate("dfsjkj"));




    }



}
