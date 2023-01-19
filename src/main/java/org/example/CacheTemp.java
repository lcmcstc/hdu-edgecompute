package org.example;

public class CacheTemp {
    /**
     * 缓存内容
     */
    public String value;

    /**
     * 解决方案层级
     */
    public int layer;

    public CacheTemp(String v,int l){
        this.layer=l;
        this.value=v;
    }
}
