package org.example;

import java.util.LinkedList;
import java.util.Map;

public class DfsResultHomogenization {
    /**
     * 当前最优解
     * key：内容  value：缓存位置
     */
    public Map<String, LinkedList<Integer>> ret;
    /**
     * 当前最优解的量化数值（总路由长度）
     */
    public long current;
}
