package org.example;

import java.util.Iterator;
import java.util.LinkedList;

public class Response {
    //路由
    public LinkedList<Integer> path;

    //路由迭代器
    public Iterator<Integer> iterator;

    //请求当前所在节点
    public int current;

    //请求的内容对象
    public String value;

    //请求经过的跳数
    public int hop;


    public Response(int start,int end,String v,PathLength pathLength){
        this.path=pathLength.getShortestPath(start,end);
        this.iterator=this.path.iterator();
        this.current=start;
        this.hop=0;
        iterator.next();
        this.value=v;
    }

    public Response(int start,int end,String v,PathLength pathLength,int hop){
        this.path=pathLength.getShortestPath(start,end);
        this.iterator=this.path.iterator();
        this.current=start;
        this.hop=hop;
        iterator.next();
        this.value=v;
    }

    public int next(){
        return this.iterator.next();
    }

}
