package org.example;

import java.util.LinkedList;

/**
 * 响应到达节点，节点递归向下询问子域节点，是否可以缓存内容，递归过程中，需要带入请求路径信息
 */
public class CacheRequest {
    public String v;
    /**
     * 来自上一个节点的路由信息
     */
    public LinkedList<Integer> pLengths;
}
