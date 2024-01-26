package com.popj;

public class Student {
        private String sid;
        private String sname;
        private String sbirth;
        private String ssex;
        private Course course;

    public Student(String sid, String sname, String sbirth, String ssex, Course course) {
        this.sid = sid;
        this.sname = sname;
        this.sbirth = sbirth;
        this.ssex = ssex;
        this.course = course;
    }

    public Student() {
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public String getSname() {
        return sname;
    }

    public void setSname(String sname) {
        this.sname = sname;
    }

    public String getSbirth() {
        return sbirth;
    }

    public void setSbirth(String sbirth) {
        this.sbirth = sbirth;
    }

    public String getSsex() {
        return ssex;
    }

    public void setSsex(String ssex) {
        this.ssex = ssex;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    @Override
    public String toString() {
        return "Student{" +
                "sid='" + sid + '\'' +
                ", sname='" + sname + '\'' +
                ", sbirth='" + sbirth + '\'' +
                ", ssex='" + ssex + '\'' +
                ", course=" + course +
                '}';
    }
}
