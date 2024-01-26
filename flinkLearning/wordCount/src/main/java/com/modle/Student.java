package com.modle;


import java.sql.Timestamp;

/**
 * @author pengshilin
 * @date 2023/5/20 1:03
 */
public class Student  {
    public String name;
    public Long timestamp;
    public int num;

    public Student() {
    }

    public Student(String name, Long timestamp, int num) {
        this.name = name;
        this.timestamp = timestamp;
        this.num = num;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }

    @Override
    public String toString() {
        return "Student{" +
                "name='" + name + '\'' +
                ", timestamp=" + new Timestamp(timestamp) +
                ", num=" + num +
                '}';
    }
}
