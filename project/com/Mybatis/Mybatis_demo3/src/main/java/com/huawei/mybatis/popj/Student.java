package com.huawei.mybatis.popj;

public class Student {
    private int sid;
    private String studentName;
    private int studentAge;
    private double studentScore;
    private int studentClass;
    private Class aClass;

    public Student(int sid, String studentName, int studentAge, double studentScore, int studentClass, Class aClass) {
        this.sid = sid;
        this.studentName = studentName;
        this.studentAge = studentAge;
        this.studentScore = studentScore;
        this.studentClass = studentClass;
        this.aClass = aClass;
    }

    public Student() {
    }

    public int getSid() {
        return sid;
    }

    public void setSid(int sid) {
        this.sid = sid;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public int getStudentAge() {
        return studentAge;
    }

    public void setStudentAge(int studentAge) {
        this.studentAge = studentAge;
    }

    public double getStudentScore() {
        return studentScore;
    }

    public void setStudentScore(double studentScore) {
        this.studentScore = studentScore;
    }

    public int getStudentClass() {
        return studentClass;
    }

    public void setStudentClass(int studentClass) {
        this.studentClass = studentClass;
    }

    public Class getaClass() {
        return aClass;
    }

    public void setaClass(Class aClass) {
        this.aClass = aClass;
    }

    @Override
    public String toString() {
        return "Student{" +
                "sid=" + sid +
                ", studentName='" + studentName + '\'' +
                ", studentAge=" + studentAge +
                ", studentScore=" + studentScore +
                ", studentClass=" + studentClass +
                ", aClass=" + aClass +
                '}';
    }
}
