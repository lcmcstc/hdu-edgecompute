package org.example;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Router {

    public PathLength plNoPeer;

    public PathLength plCanPeer;

    public boolean check_plNoPeer;

    public boolean check_plCanPeer;

    /**
     * 所有服务器节点
     */
    HashMap<Integer, EdgeServer> roots;

    /**
     * 用户终端节点`
     */
    HashMap<Integer,EdgeServer> users=new HashMap<>();

    /**
     * 核心服务器
     */
    public EdgeServer core;
    public int total;

    public Router(){

    }
    public Router(int total){
        this.total=total;
        int[][] pic = RouterGenerator.CreateRouterNew(total);
        this.roots=RouterGenerator.edgeServers;
        this.plNoPeer=RouterGenerator.ComputePathLength(pic);
        this.check_plNoPeer=RouterGenerator.checkAndSetPathLength(this.plNoPeer,pic);
        RouterGenerator.AllowPeerThrouth(pic);
        this.plCanPeer=RouterGenerator.ComputePathLength(pic);
        this.check_plCanPeer=RouterGenerator.checkAndSetPathLength(this.plCanPeer,pic);
        if(check_plNoPeer&&check_plCanPeer){
            core=this.roots.get(0);
            for (Map.Entry<Integer,EdgeServer> entry : roots.entrySet()) {
                if(entry.getKey()>=0&&entry.getKey()<total&&entry.getValue().children.size()==0){
                    users.put(entry.getKey(),entry.getValue());
                }
            }
        }

    }

    /**
     * 根据编号获取边缘服务器
     * @param number
     * @return
     */
    public EdgeServer getEdgeServer(int number){
        if(number<0||number>=total){
            return null;
        }else{
            return this.roots.get(number);
        }
    }


    public void initCore(PLS pls, LinkedList<Content> contents){
        this.check_plCanPeer= pls.check_plCanPeer;
        this.check_plNoPeer=pls.check_plNoPeer;
        this.roots= JSONObject.parseObject(pls.roots,new TypeReference<HashMap<Integer, EdgeServer>>() {});
        this.users=JSONObject.parseObject(pls.users,new TypeReference<HashMap<Integer, EdgeServer>>() {});
        this.core=pls.core;
        this.total= pls.total;
        this.plCanPeer=pls.plCanPeer;
        this.plNoPeer= pls.plNoPeer;

        for(Content c:contents){
            this.core.Force_Add(c.val);
        }
    }

    /**
     * 获取2个节点长度
     */
    public int getPathLength(int start,int end,PathLength pl){
        return getPath(start,end,pl).size();
    }
    /**
     * 获取2个节点路径
     */
    public LinkedList<Integer> getPath(int start,int end,PathLength pl){
        return pl.paths[start][end];
    }

    public static Router readRouter(String fileName, LinkedList<Content> contents) throws IOException {
        String data=FileUtil.read("pls.txt");
        PLS pls=JSONObject.parseObject(data,PLS.class);
        Router router=new Router();
        router.initCore(pls,contents);
        return router;
    }
}
