package org.example;

import java.util.Iterator;
import java.util.LinkedList;

public class Request {
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

    //如果是用户终端发出的请求，end一般情况下都是core
    public Request(int start,int end,String v,PathLength pathLength){
        this.path=pathLength.getShortestPath(start,end);
        this.iterator=this.path.iterator();
        this.current=start;
        this.hop=0;
        iterator.next();
    }

    public Request createUserRequest(int start,String v,PathLength pathLength){
        return null;
    }

    public int next(){
        return this.iterator.next();
    }

    public boolean isFromUser(Router router){
        return router.users.containsKey(this.path.getFirst());
    }
}
