package com.popj;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TestList {
    /*
     *集合类型
     */
    private List<Course> list;
    /**
     * 数组类型
     */
    private String[] str;

    private List<Course> list2;

    private Map<String,Course> courseMap;
    private Map<String,Course> courseMap2;





    public TestList() {
    }

    public TestList(List<Course> list, String[] str, List<Course> list2, Map<String, Course> courseMap, Map<String, Course> courseMap2) {
        this.list = list;
        this.str = str;
        this.list2 = list2;
        this.courseMap = courseMap;
        this.courseMap2 = courseMap2;
    }



    public List<Course> getList() {
        return list;
    }

    public void setList(List<Course> list) {
        this.list = list;
    }

    public String[] getStr() {
        return str;
    }

    public void setStr(String[] str) {
        this.str = str;
    }

    public List<Course> getList2() {
        return list2;
    }

    public void setList2(List<Course> list2) {
        this.list2 = list2;
    }

    public Map<String, Course> getCourseMap() {
        return courseMap;
    }

    public void setCourseMap(Map<String, Course> courseMap) {
        this.courseMap = courseMap;
    }

    public Map<String, Course> getCourseMap2() {
        return courseMap2;
    }

    public void setCourseMap2(Map<String, Course> courseMap2) {
        this.courseMap2 = courseMap2;
    }

    @Override
    public String toString() {
        return "TestList{" +
                "list=" + list +
                ", str=" + Arrays.toString(str) +
                ", list2=" + list2 +
                ", courseMap=" + courseMap +
                ", courseMap2=" + courseMap2 +
                '}';
    }
}
