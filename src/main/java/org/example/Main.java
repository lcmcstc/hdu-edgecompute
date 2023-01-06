package org.example;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

public class Main {
    public static void main(String[] args) {
        //满足二八定律
        ZipfGenerator zipf = new ZipfGenerator(200,1.135);
        LinkedList<Content> contents = zipf.contents;
        /*  重新计算 */
        int[][] pic = RouterGenerator.CreateRouterNew(200);
        HashMap<Integer, EdgeServer> edgeServers=RouterGenerator.edgeServers;
        /*计算路径距离(不允许同级调用)*/
        PathLength pathLength=RouterGenerator.ComputePathLength(pic);
        RouterGenerator.checkPathLength(pathLength,pic);
        //LinkedList<Integer> ppp=pathLength.getShortestPath(11,199);
        int[][] dp1 = pathLength.dp;
        /*计算路径距离，允许同级调用*/
        RouterGenerator.AllowPeerThrouth(pic);
        int[][] dp2 = RouterGenerator.ComputePathLength(pic).dp;

        //核心节点需要有全部内容
        EdgeServer core = edgeServers.get(0);
        for(Content c : contents){
            core.Force_Add(c.val);
        }
        //找到无下游节点的节点，这些节点用来充当用户终端
        LinkedList<EdgeServer> users = new LinkedList<EdgeServer>();
        for (EdgeServer edge : edgeServers.values()) {
            if (edge.children.size() == 0) {
                users.add(edge);
            }
        }
        Random random = new Random();
        //随机发出内容
        while (true) {
            int u = random.nextInt(users.size());
            EdgeServer eu = edgeServers.get(u);
            break;
        }
        System.out.println("Hello world!");
    }
}