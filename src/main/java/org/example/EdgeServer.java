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
    //链表，用于LRU
    public LinkedList<String> link = new LinkedList<>();
    //内容热度表，用于记录每个内容在本节点以及下面子节点的流行程度
    HashMap<String, Integer> hotTable = new HashMap<>();

    //如果是汇聚层或者边缘层，下游节点集合
    public LinkedList<Integer> children = new LinkedList<>();
    //上游节点
    public int parent;

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
     * 收到请求，找到最短路径，然后
     */

}
