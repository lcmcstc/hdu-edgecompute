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

    /**
     * 访问记录数量（流行度）
     */
    public int count;

    public CacheTemp(String v,int l,int c){
        this.layer=l;
        this.value=v;
        this.count=c;
    }
}
