package org.example;

import com.alibaba.fastjson.JSONObject;


public class PLS {
    public PathLength plNoPeer;

    public PathLength plCanPeer;

    public boolean check_plNoPeer;

    public boolean check_plCanPeer;

    /**
     * 所有服务器节点
     */
    public String roots;

    public int total;
    public PLS(){

    }
    public PLS(Router router){
        this.check_plCanPeer= router.check_plCanPeer;
        this.check_plNoPeer=router.check_plNoPeer;
        this.roots= JSONObject.toJSONString(router.roots);
        this.total= router.total;
        this.plCanPeer=router.plCanPeer;
        this.plNoPeer= router.plNoPeer;
    }
}
