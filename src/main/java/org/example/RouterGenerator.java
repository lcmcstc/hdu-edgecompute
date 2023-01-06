package org.example;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

public class RouterGenerator {
    static HashMap<Integer,EdgeServer> edgeServers;
    //创建新路由，且不允许同级传递
    public static int[][] CreateRouterNew(int num) {
        int[][] pic = new int[num+1][num+1];
        for (int i = 0; i < num + 1; i++) {
            for (int j = i+1; j < num + 1; j++) {
                pic[i][j] = 16;
                pic[j][i] = 16;
            }
        }
        //数字越小，层级越高
        edgeServers = new HashMap<>();
        LinkedList<EdgeServer> temp = new LinkedList<>();
        LinkedList<EdgeServer> temp2 = new LinkedList<>();
        temp.add(new EdgeServer(0, 0));
        int seq = 1;
        int level = 0;
        Random r = new Random();
        while (temp.size() > 0||temp2.size()>0) {
            level++;
            //越上级，下游节点数量越少，越下级，下游节点数量越多
            int cnum = r.nextInt(level+1)+1;
            if (temp.size() > 0)
            {
                while (temp.size() > 0)
                {
                    EdgeServer f = temp.removeFirst();
                    for (int i = 1; i <= cnum && seq < num; i++)
                    {
                        EdgeServer edgeServer = new EdgeServer(seq++, level);
                        edgeServer.parent = f.seq;
                        temp2.add(edgeServer);
                        f.children.add(edgeServer.seq);
                        pic[f.seq][edgeServer.seq] = 1;
                        pic[edgeServer.seq][f.seq] = 1;
                    }
                    edgeServers.put(f.seq,f);
                }
            }
            else {
                while (temp2.size() > 0)
                {
                    EdgeServer f = temp2.removeFirst();
                    for (int i = 1; i <= cnum && seq < num; i++)
                    {
                        EdgeServer edgeServer = new EdgeServer(seq++, level);
                        edgeServer.parent = f.seq;
                        temp.add(edgeServer);
                        f.children.add(edgeServer.seq);
                        pic[f.seq][edgeServer.seq] = 1;
                        pic[edgeServer.seq][f.seq] = 1;
                    }
                    edgeServers.put(f.seq, f);
                }
            }
        }
        return pic;
    }

    //允许同级传递
    public static void AllowPeerThrouth(int[][] pic) {
        for (EdgeServer e : edgeServers.values()) {
            if (e.children.size() > 0)
            {
                int b = e.children.getFirst();
                int l = e.children.getLast();
                for (int i = b + 1; i <= l; i = b + 1)
                {
                    pic[b][i] = 1;
                    pic[i][b] = 1;
                    b = i;
                }
            }
        }
    }

    public static PathLength ComputePathLength(int[][] pic)
    {
        PathLength ret=new PathLength();
        int col = pic.length;
        int[][] dp = new int[col][col];
        int[][] path=new int[col][col];
        for (int i = 0; i < col; i++)
        {
            for (int j = 0; j < col; j++)
            {
                dp[i][j] = pic[i][j];
                path[i][j]=j;
            }
        }
        for (int k = 0; k < col; k++) {
            for (int s = 0; s < col; s++) {
                for (int e = 0; e < col; e++) {
                    if(dp[s][k]+dp[k][e]<dp[s][e]){
                        dp[s][e]=dp[s][k]+dp[k][e];
                        //说明从s到e经过中介点k能提供更短距离
                        path[s][e]=k;
                    }
                }
            }
        }
        ret.dp=dp;
        ret.midPoint =path;
        return ret;
    }

    public static LinkedList<Integer> getPath(int start,int end,PathLength pathLength){
        if(pathLength.paths[start][end]!=null){
            return pathLength.paths[start][end];
        }
        LinkedList<Integer> ret=new LinkedList<>();
        if(pathLength.midPoint[start][end]!=end){
            ret.addAll(getPath(start,pathLength.midPoint[start][end],pathLength));
            ret.removeLast();
            ret.addAll(getPath(pathLength.midPoint[start][end],end,pathLength));
        }else{
            ret.add(start);
            ret.add(end);
        }
        StringBuilder stringBuilder=new StringBuilder();
        for(int i:ret){
            stringBuilder.append(i).append('-');
        }
        stringBuilder.setLength(stringBuilder.length()-1);
        System.out.println("从"+start+"到"+end+"的最短路径长度为："+
                pathLength.dp[start][end]+"。路径为:"+stringBuilder.toString());
        pathLength.paths[start][end]=ret;
        return ret;
    }
    public static boolean checkPathLength(PathLength pathLength,int[][] pic){
        pathLength.pic=pic;
        pathLength.paths=new LinkedList[pathLength.dp.length][pathLength.dp.length];
        for(int start=0;start<pathLength.dp.length;start++){
            for(int end=start+1;end<pathLength.dp.length;end++){
                getPath(start,end,pathLength);
            }
        }
        return true;
    }
}
