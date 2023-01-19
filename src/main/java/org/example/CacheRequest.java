package org.example;

/**
 * 响应到达节点，节点递归向下询问子域节点，是否可以缓存内容，递归过程中，需要带入请求路径信息
 */
public class CacheRequest {
    /**
     * 应该缓存的内容
     */
    public String value;
    /**
     * 缓存解的层级
     */
    public int layer;
}
