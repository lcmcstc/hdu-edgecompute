package org.example;

import com.alibaba.fastjson.JSONObject;

import java.util.Map;
import java.util.TreeMap;

public class bak {
    //统计路径
    public void countPATH(){
//        TreeMap<Integer,Integer> count1=new TreeMap<>();
//        //统计终端的路径长度
//        for(Map.Entry<Integer,EdgeServer> entry:router.users.entrySet()){
//            for(Map.Entry<Integer,EdgeServer> entry1:router.roots.entrySet()){
//                if(router.users.containsKey(entry1.getKey())||((int)entry.getKey())==((int)entry1.getKey())){
//                    continue;
//                }
//                int pathLength=router.getPathLength(entry.getKey(),entry1.getKey(), router.plNoPeer);
//                if(count1.containsKey(pathLength)){
//                    count1.put(pathLength,count1.get(pathLength)+1);
//                }else{
//                    count1.put(pathLength,1);
//                }
//            }
//        }
//        TreeMap<Integer,Integer> count2=new TreeMap<>();
//        //统计终端的路径长度
//        for(Map.Entry<Integer,EdgeServer> entry:router.users.entrySet()){
//            for(Map.Entry<Integer,EdgeServer> entry1:router.roots.entrySet()){
//                if(router.users.containsKey(entry1.getKey())||((int)entry.getKey())==((int)entry1.getKey())){
//                    continue;
//                }
//                int pathLength=router.getPathLength(entry.getKey(),entry1.getKey(), router.plCanPeer);
//                if(count2.containsKey(pathLength)){
//                    count2.put(pathLength,count2.get(pathLength)+1);
//                }else{
//                    count2.put(pathLength,1);
//                }
//            }
//        }
    }

    //创建路由
    public void createRouter(){
        //        int num=1000;
//        Router router=new Router(num);
//        FileUtil.write("pls.txt",JSONObject.toJSONString(new PLS(router)));
    }

    //读区路由
    public void readRouter(){
//        String data=FileUtil.read("pls.txt");
//        PLS pls= JSONObject.parseObject(data,PLS.class);
//        Router router=new Router();
//        router.initCore(pls,contents);
    }
}
