package org.example;

import java.io.*;
import java.util.*;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        /*----------------------------创建满足齐普夫定律的内容-------------------------------*/
        //满足二八定律
        ZipfGenerator zipf = new ZipfGenerator(200,1.135);
        /*----------------------------创建满足齐普夫定律的内容完毕-------------------------------*/

        /*------------------------------创建路由-------------------------------------------*/
        Router router=Router.readRouter("pls.txt",zipf);
        //Router router=Router.createRouter(200,zipf);
        /*------------------------------路由创建完毕-------------------------------------------*/

        /**
         * db是记录每个用户访问每个内容的跳数
         */
        HashMap<EdgeServer,HashMap<String,LinkedList<Integer>>> db=new HashMap<>();
        //记录每个用户的请求总数
        HashMap<Integer,Integer> m1=new HashMap<>();
        //记录每个用户的平均访问长度
        HashMap<Integer,LinkedList<Double>> m2=new HashMap<>();
        //记录全局平均请求跳数
        LinkedList<Double> avg_hop=new LinkedList<>();
        //随机发出内容
        Thread randRequest=new Thread(new Runnable() {
            @Override
            public void run() {
                long start=System.currentTimeMillis();
                int times=0;
                double total_times=0.0;
                double total_hop=0.0;
                while(true) {
                    EdgeServer user = router.getRandomUser();
                    Request request=Request.createUserRequest(user.seq,zipf.getRandomContentByGap(user.gap).val, router.plNoPeer);
                    Response response=router.getEdgeServer(request.next()).receiveRequest(request,router);
                    int hop=response.hop+response.hop_request;
                    if(db.containsKey(user)){
                        HashMap<String,LinkedList<Integer>> m=db.get(user);
                        if(m.containsKey(request.value)){
                            m.get(request.value).add(hop);
                        }else{
                            LinkedList<Integer> l=new LinkedList<>();
                            l.add(hop);
                            m.put(request.value,l);
                        }
                    }else{
                        HashMap<String,LinkedList<Integer>> m=new HashMap<>();
                        LinkedList<Integer> l=new LinkedList<>();
                        l.add(hop);
                        m.put(request.value, l);
                        db.put(user,m);
                    }
                    System.out.println("用户 "+user.seq+" 发出内容 "+request.value+" 的请求，共经历跳数 "+hop);
                    if(m1.containsKey(user.seq)){
                        int tu=m1.get(user.seq);
                        double total=m2.get(user.seq).getLast()*tu;
                        total+=hop;
                        tu++;
                        m2.get(user.seq).add(total/(tu*1.0));
                    }else{
                        m1.put(user.seq,1);
                        LinkedList<Double> list=new LinkedList<>();
                        list.add(hop*1.0);
                        m2.put(user.seq,list);
                    }
                    total_times++;
                    total_hop+= hop;
                    times++;
                    if(times%100==0){
                        for(EdgeServer edgeServer:router.roots.values()){
                            if(!router.users.containsKey(edgeServer.seq)){
                                edgeServer.solution(router);
                            }
                        }
                        avg_hop.add(total_hop*2/total_times);
                    }
                    if(times%500==0){
                        for(EdgeServer edgeServer:router.roots.values()){
                            if(!router.users.containsKey(edgeServer.seq)){
                                edgeServer.updateRecords();
                            }
                        }
                    }
                    if(times==200000){
                        long end=System.currentTimeMillis();
                        System.out.println("模拟发送133333个内容共耗时 "+(end-start)/1000+" 秒");
                        System.out.println("最初平均跳数  "+avg_hop.getFirst()+",最终平均跳数  "+avg_hop.getLast());
                        System.out.println(avg_hop);
                        break;
                    }
//                    try {
//                        Thread.sleep(10);
//                    } catch (InterruptedException e) {
//                        throw new RuntimeException(e);
//                    }
                }
            }
        });
        randRequest.start();
        //System.out.println("Hello world!");
    }
}