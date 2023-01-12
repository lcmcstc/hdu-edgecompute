package org.example;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        /*----------------------------创建满足齐普夫定律的内容-------------------------------*/
        //满足二八定律
        ZipfGenerator zipf = new ZipfGenerator(200,1.135);
        LinkedList<Content> contents = zipf.contents;
        /*----------------------------创建满足齐普夫定律的内容完毕-------------------------------*/

        /*------------------------------创建路由-------------------------------------------*/
        Router router=Router.readRouter("pls.txt",zipf);
        /*------------------------------路由创建完毕-------------------------------------------*/

        Random random = new Random();
        //随机发出内容
        while (true) {
            EdgeServer user = router.getRandomUser();
            if(user==null){
                continue;
            }
            Request request=Request.createUserRequest(user.seq,zipf.getRandomContentByGap(user.gap).val, router.plNoPeer);
            Response response=router.getEdgeServer(request.next()).reciveRequest(request,router);
            int r=-1;
        }
        //System.out.println("Hello world!");
    }
}