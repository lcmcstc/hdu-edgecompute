package org.example;

public class Record {
    /**
     * 请求内容
     */
    public String value;
    /**
     * 请求到达时间
     */
    public long arriveTime;
    /**
     * 请求发出方
     */
    public int fromUser;

    public Record(String v,long t,int u){
        this.value=v;
        this.fromUser=u;
        this.arriveTime=t;
    }
}
