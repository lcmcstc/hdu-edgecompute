package org.example;

/**
 * 随机内容测试
 */
public class Temp {
    public int times;
    public String val;
    public Temp(String v,int t){
        this.val=v;
        this.times=t;
    }
    @Override
    public String toString(){
        return val+":"+times;
    }
}
