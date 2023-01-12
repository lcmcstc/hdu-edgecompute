package org.example;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;

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


    public void initCore(PLS pls,ZipfGenerator zipfGenerator){
        this.check_plCanPeer= pls.check_plCanPeer;
        this.check_plNoPeer=pls.check_plNoPeer;
        this.roots= JSONObject.parseObject(pls.roots,new TypeReference<HashMap<Integer, EdgeServer>>() {});
        this.total= pls.total;
        this.plCanPeer=pls.plCanPeer;
        this.plNoPeer= pls.plNoPeer;
        core=this.roots.get(0);
        for (Map.Entry<Integer,EdgeServer> entry : roots.entrySet()) {
            if(entry.getKey()>=0&&entry.getKey()<total&&entry.getValue().children.size()==0){
                users.put(entry.getKey(),entry.getValue());
            }
        }
        for(Content c:zipfGenerator.contents){
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

    public static Router readRouter(String fileName,ZipfGenerator zipfGenerator) throws IOException {
        String data=FileUtil.read(fileName);
        PLS pls=JSONObject.parseObject(data,PLS.class);
        Router router=new Router();
        router.initCore(pls,zipfGenerator);
        return router;
    }

    public static void writeRouter(String fileName,int num,ZipfGenerator zipfGenerator) throws IOException {
        Router router=new Router(num);
        router.distributeGap(zipfGenerator);
        String str=JSONObject.toJSONString(new PLS(router));
        FileUtil.write(fileName,str);
    }

    Random random111 = new Random(111);
    public EdgeServer getRandomUser(){
        int userSeq = random111.nextInt(users.size());
        int i=0;
        for(Map.Entry<Integer,EdgeServer> entry:users.entrySet()){
            if(i++==userSeq){
                return entry.getValue();
            }
        }
        return null;
    }

    public void distributeGap(ZipfGenerator zipfGenerator){
        final Random random = new Random(0);
        for(EdgeServer user: users.values()){
            user.gap=zipfGenerator.gaps[random.nextInt(zipfGenerator.gap_contents.keySet().size())];
        }
    }

}
