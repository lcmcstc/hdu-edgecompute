package org.example;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

public class EdgeServer {
    public EdgeServer(int seq, int level) {
        this.seq = seq;
        this.level = level;
    }

    public EdgeServer(){

    }
    public int seq;
    /**
     * 0 核心层、1 汇聚层、2 边缘层   3 通用层
     */
    public int level;
    //键是内容。值是节点索引
    HashMap<String, Integer> DHT = new HashMap<>();
    //本地缓存
    HashSet<String> cache = new HashSet<>();
    //缓存容量
    public int caption = 10;
    //链表，仅用于LRU
    public LinkedList<String> link = new LinkedList<>();
    //内容热度表，用于记录每个内容在本节点以及下面子节点的流行程度
    HashMap<String, Integer> hotTable = new HashMap<>();

    //如果是汇聚层或者边缘层，下游节点集合
    public LinkedList<Integer> children = new LinkedList<>();
    //上游节点
    public int parent;

    //偏好,仅针对用户终端使用
    public char gap;

    //无条件添加缓存内容
    public void Force_Add(String cont) {
        this.cache.add(cont);
    }

    public void LRU_ADD(String content) {
        if (cache.contains(content)) {
            link.remove(content);
            link.addFirst(content);
            return;
        }
        if (cache.size() < this.caption) {
            link.addFirst(content);
            cache.add(content);
            return;
        }
        cache.remove(link.removeLast());
        cache.add(content);
        link.addFirst(content);
    }


    //每个边缘服务器基础动作 1、收到请求  2、转发请求 3、收到响应 4、转发响应
    //缓存动作 1、询问下游子节点  2、做出决策 3、缓存
    /**
     * 收到请求，并给出响应。是否缓存则在收到响应之后决定
     * 收到请求一共有3条路
     * 1、本地有缓存，直接满足响应。（直接生成响应向下发送）
     * 2、本地没有缓存，但是有联合缓存。生成新的请求，起点为当前节点，终点为有数据的缓存点。然后递归
     * 3、本地没有缓存，且没有联合缓存，向上转发请求。
     *
     * 内容热度表更新分2条路走，（防止重复计算） 只有来自于终端的请求才会更新热度表
     * 1、请求到达节点时，立即更新本地hotTable表
     * 2、若当前节点能满足请求，则需要递归通知当前节点父节点更新hotTable表
     */
    public Response reciveRequest(Request request,Router router){
        request.hop++;
        request.current=this.seq;
        if(request.isFromUser(router)) {
            updateHot(request.value);
        }
        if(this.cache.contains(request.value)){
            //本节点已经满足缓存，直接返回。创建新的响应，并向下发送
            Response response=new Response(this.seq,request.path.getFirst(),request.value, router.plNoPeer);
            if(request.isFromUser(router)) {
                updateHotRecurrence(request.value, router);
            }
            return router.getEdgeServer(response.next()).reciveResponse(response,router);
        }
        if(DHT.containsKey(request.value)){
            Request myReq=new Request(this.seq,DHT.get(request.value),request.value,router.plNoPeer);
            Response respTemp=router.getEdgeServer(request.next()).reciveRequest(myReq,router);
            //收到响应后，生成响应给用户的响应
            Response response=new Response(this.seq,request.path.getFirst(),request.value, router.plNoPeer,respTemp.hop);
            if(request.isFromUser(router)) {
                updateHotRecurrence(request.value, router);
            }
            return router.getEdgeServer(response.next()).reciveResponse(response,router);
        }
        return router.getEdgeServer(request.next()).reciveRequest(request,router);
    }

    /**
     * 节点收到响应实际上只有2件事
     * 1、继续向链路下游转发响应 （响应也有跳数）
     * 2、决定是否缓存响应内容，以及缓存在什么位置 //TODO （整个实验的核心）
     */
    public Response reciveResponse(Response response,Router router){
        response.hop++;
        response.current=this.seq;
        if(this.seq==response.path.getLast()){
            //响应已经到达目的地
            return response;
        }
        return router.getEdgeServer(response.next()).reciveResponse(response,router);
    }

    public void updateHot(String v){
        if(hotTable.containsKey(v)){
            hotTable.put(v,hotTable.get(v)+1);
        }else{
            hotTable.put(v,1);
        }
    }

    public void updateHotRecurrence(String v,Router router){
        if(this.seq!=this.parent){
            //首先更新父节点hotTable，然后调用父节点的addHotRecurrence
            EdgeServer parent=router.getEdgeServer(this.parent);
            parent.updateHot(v);
            parent.updateHotRecurrence(v,router);
        }
    }


    //缓存之后要递归更新父级的DHT
}
