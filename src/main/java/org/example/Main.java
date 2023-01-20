package org.example;

import java.io.*;
import java.util.*;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        /*----------------------------创建满足齐普夫定律的内容-------------------------------*/
        //满足二八定律
        ZipfGenerator zipf = new ZipfGenerator(200,1.135);
        LinkedList<Content> contents = zipf.contents;
        /*----------------------------创建满足齐普夫定律的内容完毕-------------------------------*/

        /*------------------------------创建路由-------------------------------------------*/
        //Router router=Router.readRouter("pls.txt",zipf);
        Router router=new Router(20);
        /*------------------------------路由创建完毕-------------------------------------------*/

        /**
         * db是记录每个用户访问每个内容的跳数
         */
        HashMap<EdgeServer,HashMap<String,LinkedList<Integer>>> db=new HashMap<>();
        Random random = new Random();
        //随机发出内容
        while (true) {
            EdgeServer user = router.getRandomUser();
            if(user==null){
                continue;
            }
            Request request=Request.createUserRequest(user.seq,zipf.getRandomContentByGap(user.gap).val, router.plNoPeer);
            Response response=router.getEdgeServer(request.next()).receiveRequest(request,router);
            int hop=Math.min(response.hop, request.path.size()-1);
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
            int r=-1;
        }
        //System.out.println("Hello world!");
    }
}