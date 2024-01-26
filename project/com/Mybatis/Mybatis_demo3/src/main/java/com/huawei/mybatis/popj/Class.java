package com.huawei.mybatis.popj;

import java.util.List;

public class Class {
    private int cid;
    private String ClassName;
    private List<Student> students;

    public List<Student> getStudents() {
        return students;
    }

    public void setStudents(List<Student> students) {
        this.students = students;
    }

    public Class() {
    }

    public Class(int cid, String className, List<Student> students) {
        this.cid = cid;
        ClassName = className;
        this.students = students;
    }

    @Override
    public String toString() {
        return "Class{" +
                "cid=" + cid +
                ", ClassName='" + ClassName + '\'' +
                ", students=" + students +
                '}';
    }

    public int getCid() {
        return cid;
    }

    public void setCid(int cid) {
        this.cid = cid;
    }

    public String getClassName() {
        return ClassName;
    }

    public void setClassName(String className) {
        ClassName = className;
    }
}
