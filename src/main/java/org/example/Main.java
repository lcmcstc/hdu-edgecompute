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
        //Router router=Router.readRouter("pls.txt",zipf);
        Router router=Router.createRouter(20,zipf);
        /*------------------------------路由创建完毕-------------------------------------------*/

        /**
         * db是记录每个用户访问每个内容的跳数
         */
        HashMap<EdgeServer,HashMap<String,LinkedList<Integer>>> db=new HashMap<>();
        //随机发出内容

        int times=0;
        Thread randRequest=new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    EdgeServer user = router.getRandomUser();
                    Request request=Request.createUserRequest(user.seq,zipf.getRandomContentByGap(user.gap).val, router.plNoPeer);
                    Response response=router.getEdgeServer(request.next()).receiveRequest(request,router);
                    int hop=response.hop;
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
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        randRequest.start();
        Thread.sleep(10000);
        for(EdgeServer edgeServer:router.roots.values()){
            if(!router.users.containsKey(edgeServer.seq)){
                edgeServer.solution(router);
            }
            Thread.sleep(10000);
        }
        //System.out.println("Hello world!");
    }
}