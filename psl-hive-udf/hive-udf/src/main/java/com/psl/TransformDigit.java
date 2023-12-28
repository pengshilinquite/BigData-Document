package com.psl;

import org.apache.hadoop.hive.ql.exec.UDF;


import java.util.HashMap;
import java.util.Map;

/**
 * @author pengshilin
 * @date 2023/12/28 21:08
 * 将中文数字转为阿拉伯数字
 * create function transformDigit as 'com.psl.TransformDigit' using jar 'hdfs:/user/warehouse/hive-udf-1.0-SNAPSHOT.jar';
 * select transformDigit("六仟零柒元整壹角肆分")
 */
public class TransformDigit extends UDF {
    private static final Map<Character,Double> CHINESE_DIGIT_MAP = new HashMap<Character, Double>();
    static {
        CHINESE_DIGIT_MAP.put('零', 0.00);
        CHINESE_DIGIT_MAP.put('一', 1.00);
        CHINESE_DIGIT_MAP.put('壹', 1.00);
        CHINESE_DIGIT_MAP.put('二', 2.00);
        CHINESE_DIGIT_MAP.put('贰', 2.00);
        CHINESE_DIGIT_MAP.put('三', 3.00);
        CHINESE_DIGIT_MAP.put('叁', 3.00);
        CHINESE_DIGIT_MAP.put('四', 4.00);
        CHINESE_DIGIT_MAP.put('肆', 4.00);
        CHINESE_DIGIT_MAP.put('五', 5.00);
        CHINESE_DIGIT_MAP.put('伍', 5.00);
        CHINESE_DIGIT_MAP.put('六', 6.00);
        CHINESE_DIGIT_MAP.put('陆', 6.00);
        CHINESE_DIGIT_MAP.put('七', 7.00);
        CHINESE_DIGIT_MAP.put('柒', 7.00);
        CHINESE_DIGIT_MAP.put('八', 8.00);
        CHINESE_DIGIT_MAP.put('捌', 8.00);
        CHINESE_DIGIT_MAP.put('九', 9.00);
        CHINESE_DIGIT_MAP.put('玖', 9.00);
        CHINESE_DIGIT_MAP.put('十', 10.00);
        CHINESE_DIGIT_MAP.put('拾', 10.00);

    }
    private static final Map<Character,Double> CHINESE_UNIT_MAP = new HashMap<Character, Double>();
    static {
        CHINESE_UNIT_MAP.put('分', 0.01);
        CHINESE_UNIT_MAP.put('角', 0.10);
        CHINESE_UNIT_MAP.put('毛', 0.10);
        CHINESE_UNIT_MAP.put('元', 1.00);
        CHINESE_UNIT_MAP.put('圆', 1.00);
        CHINESE_UNIT_MAP.put('十', 10.00);
        CHINESE_UNIT_MAP.put('拾', 10.00);
        CHINESE_UNIT_MAP.put('百', 100.00);
        CHINESE_UNIT_MAP.put('佰', 100.00);
        CHINESE_UNIT_MAP.put('千', 1000.00);
        CHINESE_UNIT_MAP.put('仟', 1000.00);
        CHINESE_UNIT_MAP.put('万', 10000.00);
        CHINESE_UNIT_MAP.put('萬', 10000.00);
        CHINESE_UNIT_MAP.put('亿', 100000000.00);
    }
    public String evaluate(String number){
        double result = 0.00;
        double currentNumber = 0.00;
        double lastNumber = 0.00;
        char[] chars = number.replace("整","").toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if(CHINESE_DIGIT_MAP.containsKey(chars[i])){
                currentNumber = CHINESE_DIGIT_MAP.get(chars[i]);
            }else if (CHINESE_UNIT_MAP.containsKey(chars[i])){
                result+=lastNumber*CHINESE_UNIT_MAP.get(chars[i]);
                lastNumber = 0;
            }
            if (currentNumber > 0) {
                lastNumber = currentNumber;
                currentNumber = 0.00;
            }

        }
        result+=lastNumber;
        return String.valueOf(result);
    }

    public static void main(String[] args) {
        TransformDigit transformDigit = new TransformDigit();
        String number = transformDigit.evaluate("六仟零柒元整壹角肆分");
        System.out.println(number);

    }
}
