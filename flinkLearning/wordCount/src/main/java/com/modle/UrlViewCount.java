package com.modle;

import java.sql.Timestamp;

/**
 * @author pengshilin
 * @date 2023/5/22 21:04
 */
public class UrlViewCount {
    public String url;
    public Long count;

    public Long start;
    public Long end;

    public UrlViewCount(String url, Long count, Long start, Long end) {
        this.url = url;
        this.count = count;
        this.start = start;
        this.end = end;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public Long getStart() {
        return start;
    }

    public void setStart(Long start) {
        this.start = start;
    }

    public Long getEnd() {
        return end;
    }

    public void setEnd(Long end) {
        this.end = end;
    }

    @Override
    public String toString() {
        return "UrlViewCount{" +
                "url='" + url + '\'' +
                ", count=" + count +
                ", start=" + start +
                ", end=" + end +
                '}';
    }
}
