package com.popj;

public class Course {
    private String cid;
    private String cname;
    private String tid;

    @Override
    public String toString() {
        return "Course{" +
                "cid='" + cid + '\'' +
                ", cname='" + cname + '\'' +
                ", tid='" + tid + '\'' +
                '}';
    }

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getCname() {
        return cname;
    }

    public void setCname(String cname) {
        this.cname = cname;
    }

    public String getTid() {
        return tid;
    }

    public void setTid(String tid) {
        this.tid = tid;
    }

    public Course() {
    }

    public Course(String cid, String cname, String tid) {
        this.cid = cid;
        this.cname = cname;
        this.tid = tid;
    }
}
