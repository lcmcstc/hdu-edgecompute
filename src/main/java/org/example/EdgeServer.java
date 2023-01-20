package org.example;

import java.util.*;
import java.util.stream.Collectors;

public class EdgeServer {
    /**
     * 涉及到迭代器，且线程不安全时，不能锁，统一采用
     */

    /**
     * 同质化缓存阈值 （s*c）
     */
    public final int limitHomogenization=12;

    /**
     * 异质化缓存阈值（s^c）
     */
    public final int limitHeterogenization=24;

    /**
     * 统计记录时间的区间
     */
    public final long interval4updateRecords =3*60*1000;

    /**
     * 更新访问记录，清除历史访问记录，保留统计区间内容的访问记录。（加入随机冷冻算法）(可用线程处理) TODO
     */
    public void updateRecords(){
        Random r=new Random();
        LinkedList<Record> ret=new LinkedList<>();
        for(Record record:records){
            double x=(((System.currentTimeMillis()-record.arriveTime)+ interval4updateRecords)*1.0);
            double y=(interval4updateRecords *1.0);
            double d=y/x;
            if(r.nextDouble()<=d){
                ret.add(record);
            }
        }
        this.records=ret;
        if(this.records.size()>0) {
            System.out.println("节点 " + this.seq + "记录清理完毕，当前共 "
                    + this.records.size() + "条记录，最新记录时间："
                    + (System.currentTimeMillis() - this.records.getLast().arriveTime) / 1000
                    + ",最后记录时间：" + (System.currentTimeMillis() - this.records.getFirst().arriveTime) / 1000);
        }
    }

    private boolean startUpdateRecords=false;
    public void startUpdateRecords(){
        if(!startUpdateRecords){
            startUpdateRecords=true;
            Thread thread=new Thread(new Runnable() {
                @Override
                public void run() {
                    while(true){
                        try {
                            Thread.sleep(interval4updateRecords);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        updateRecords();
                    }
                }
            });
            thread.start();
        }
    }
    /**
     * 用一个数据结构存储解以及解来源。
     * 本质上就是本节点待缓存的内容，实际上已经占位，后续决策仅允许使用剩余空间。保证下次内容达到节点时，一定能被缓存
     * key:应该被缓存的内容  value:决定该内容被缓存到本节点的决策层级
     * 注意，当有新的决策被送达节点时，需要删除needCache中上次相同决策层级的所有内容，并且清除出缓存
     * 当有相同的解时，做链式层级（为了更新的时候不删除其他解）
     */
    public Map<String,HashSet<Integer>> cache=new HashMap<>();

    /**
     * 每个节点需要记录请求内容和请求来源，在缓存决策时由上游节点收集下游节点信息即可
     * 第一层： key：请求内容  value：map
     * 第二次： key：请求来源（用户终端序号）， value：统计次数
     */
    public LinkedList<Record> records=new LinkedList<>();

    /**
     * 收到缓存请求，更新本地缓存，若有冲突则解决冲突（按照缓存层级进行缓存淘汰）
     * 收到缓存请求，直接更新本地缓存，然后进行冲突解决。缓存请求应该是一个List，因为定期计算，每次计算会重新分配缓存位置与缓存内容，并不是实时的
     */
    public CacheResponse receiveCacheRequest(List<CacheOrderRequest> cacheRequests,Router router){
        int layer = cacheRequests.get(0).layer;
        LinkedList<String> needRemove = new LinkedList<>();
        for (Map.Entry<String, HashSet<Integer>> entry : this.cache.entrySet()) {
            entry.getValue().remove(layer);
            if (entry.getValue().size() == 0) {
                //说明该缓存已经失效了
                needRemove.add(entry.getKey());
            }
        }
        for (String s : needRemove) {
            this.cache.remove(s);
        }
        for (CacheOrderRequest cacheRequest : cacheRequests) {
            if (this.cache.containsKey(cacheRequest.value)) {
                this.cache.get(cacheRequest.value).add(cacheRequest.layer);
            } else {
                HashSet<Integer> set = new HashSet<>();
                set.add(cacheRequest.layer);
                this.cache.put(cacheRequest.value, set);
            }
        }
        competeCache();
        //缓存更新完毕后，通知父节点自己的缓存信息
        HashSet<String> myCache = new HashSet<>();
        for (Map.Entry<String, HashSet<Integer>> entry : this.cache.entrySet()) {
            myCache.add(entry.getKey());
        }
        EdgeServer parent = router.getEdgeServer(this.parent);
        parent.receiveAndUpdateDHT(myCache, this.seq);
        return null;
    }

    /**
     * 缓存淘汰算法（以解决方案层级为优先的缓存替换算法）
     */
    public void competeCache(){
        if (this.cache.size() > this.caption) {
            //说明当前缓存空间不足，需要发生缓存替换
            LinkedList<CacheTemp> tl=new LinkedList<>();
            HashMap<String, Integer> records_count = this.countRecord();
            for (Map.Entry<String, HashSet<Integer>> entry : cache.entrySet()) {
                int minLayer = Collections.min(entry.getValue());
                if(records_count.containsKey(entry.getKey())){
                    //失去流行度的不缓存
                    tl.add(new CacheTemp(entry.getKey(), minLayer, records_count.get(entry.getKey())));
                }

            }
            CacheTemp[] cacheTemps = tl.toArray(new CacheTemp[0]);
            //先按照内容访问热度排序，再按照解决方案层级排序，这样同层级竞争就可以按照内容热度排序(热度从大到小，层级从大到小)
            Arrays.sort(cacheTemps, (o1, o2) -> o2.count - o1.count);
            Arrays.sort(cacheTemps, (o1, o2) -> o2.layer - o1.layer);
            //只取top layer个
            Map<String, HashSet<Integer>> ret = new HashMap<>();
            for (int j = 0; j < cacheTemps.length; j++) {
                ret.put(cacheTemps[j].value, cache.get(cacheTemps[j].value));
            }
            this.cache = ret;
        }
    }

    //统计访问记录
    private HashMap<String,Integer> countRecord(){
        HashMap<String,Integer> records_count=new HashMap<>();
        for(Record record:this.records){
            c1(records_count,record);
        }
        return records_count;
    }
    private void c1(HashMap<String,Integer> records_count,Record record){
        if(records_count.containsKey(record.value)){
            records_count.put(record.value,records_count.get(record.value)+1);
        }else{
            records_count.put(record.value,1);
        }
    }
    private void c2(HashMap<String,HashMap<Integer,Integer>> records_from_count,Record record){
        if(records_from_count.containsKey(record.value)){
            if(records_from_count.get(record.value).containsKey(record.fromUser)){
                records_from_count.get(record.value).put(record.fromUser,records_from_count.get(record.value).get(record.fromUser)+1);
            }else{
                records_from_count.get(record.value).put(record.fromUser,1);
            }
        }else{
            HashMap<Integer,Integer> m1=new HashMap<>();
            m1.put(record.fromUser,1);
            records_from_count.put(record.value,m1);
        }
    }
    private void c3(Map<Integer,LinkedList<CacheOrderRequest>> result, DfsResultHeterogenization dfsResult){
        if(dfsResult!=null){
            for(Map.Entry<String,Integer> entry:dfsResult.ret.entrySet()){
                if(result.containsKey(entry.getValue())){
                    //dfs_Homogenization与dfs_Heterogenization不可能有重复的内容，直接添加
                    result.get(entry.getValue()).add(new CacheOrderRequest(entry.getKey(), this.level));
                }else{
                    LinkedList<CacheOrderRequest> l=new LinkedList<>();
                    l.add(new CacheOrderRequest(entry.getKey(), this.level));
                    result.put(entry.getValue(),l);
                }
            }
        }
    }
    private void c4(Map<Integer,LinkedList<CacheOrderRequest>> result, DfsResultHomogenization dfsResult){
        if(dfsResult!=null){
            for(Map.Entry<String,LinkedList<Integer>> entry:dfsResult.ret.entrySet()){
                for(int i:entry.getValue()) {
                    if (result.containsKey(i)) {
                        result.get(i).add(new CacheOrderRequest(entry.getKey(), this.level));
                    } else {
                        LinkedList<CacheOrderRequest> l = new LinkedList<>();
                        l.add(new CacheOrderRequest(entry.getKey(), this.level));
                        result.put(i, l);
                    }
                }
            }
        }
    }


    DfsResultHomogenization resultHomogenization=null;
    DfsResultHeterogenization resultHeterogenization=null;
    LinkedList<LinkedList<Integer>> dfs_fullArrayRet=null;
    /**
     * 缓存决策算法，局部全信息透明最优决策  定期执行 TODO
     * router是全局路由信息
     */
    public void solution(Router router){
        long start=System.currentTimeMillis();
        System.out.println("开始进入节点 "+this.seq+" 解决方案处理过程");

        /**   第一步：收集本子域内所有的信息，包括请求访问信息，以及节点数量信息  **/
        //s 表示整个子域中节点数量
        int s=this.children.size()+1;//+1是因为核节点本身也可以存储
        //k1 表示整个子域中允许同质化缓存的数量
        int k1=limitHomogenization/s;
        //k2 表示整个子域中允许异质化缓存的数量
        int k2=limitHeterogenization/s;
        //同质化缓存+异质化缓存总内容不应该超过整个子域的最大缓存空间
        if(k1+k2>s*caption){
            k2=s*caption-k1;
        }
        //records_count是所有流经本子域内容的统计
        HashMap<String,Integer> records_count=new HashMap<>();
        //records_count是所有流经本子域内容发出方的统计
        HashMap<String,HashMap<Integer,Integer>> records_from_count=new HashMap<>();
        for (Record record : this.records) {
            c1(records_count, record);
            c2(records_from_count, record);
        }
        for(int seq:this.children){
            EdgeServer edgeServer= router.getEdgeServer(seq);
            for (Record record : edgeServer.records) {
                c1(records_count, record);
                c2(records_from_count, record);
            }
        }
        //只对热度排名前k1个内容进行同质化计算，简化问题域
        //从大到小排序
        Iterator<Map.Entry<String,Integer>> iterator=records_count.entrySet().stream().sorted((o1, o2) -> o2.getValue()-o1.getValue()).iterator();
        //needDistributeHomogenization是需要进行同质化缓存的内容
        LinkedList<String> temp=new LinkedList<>();
        int rank=0;
        while(iterator.hasNext()&&rank<k1){
            temp.add(iterator.next().getKey());
            rank++;
        }
        String[] needDistributeHomogenization=temp.toArray(new String[0]);
        //needDistributeHeterogenization是需要进行异质化缓存的内容
        temp.clear();
        while(iterator.hasNext()&&rank<k1+k2){
            temp.add(iterator.next().getKey());
            rank++;
        }
        String[] needDistributeHeterogenization=temp.toArray(new String[0]);
        /*-------------------------信息收集完毕-----------------------------------*/
        long currentTime=System.currentTimeMillis();
        System.out.println("收集信息共耗时："+(currentTime-start)/1000+"秒");start=currentTime;
        /**   第二步：计算最优解  **/

        LinkedList<Integer> selectors=new LinkedList<>(this.children);
        selectors.add(this.seq);
        selectors= selectors.stream().sorted().collect(Collectors.toCollection(LinkedList::new));
        dfs_fullArrayRet=new LinkedList<>();
        dfs_fullArray(selectors,new LinkedList<>(),new HashSet<>());
        resultHomogenization=null;
        dfs_Homogenization(records_from_count,needDistributeHomogenization,0,new HashMap<>(),dfs_fullArrayRet,router);
        currentTime=System.currentTimeMillis();
        System.out.println("计算同质化缓存以及全排列共耗时："+(currentTime-start)/1000+"秒");start=currentTime;
        resultHeterogenization=null;
        dfs_Heterogenization(records_from_count,needDistributeHeterogenization,0,new HashMap<>(),selectors,router);
        currentTime=System.currentTimeMillis();
        System.out.println("计算异质化缓存以及全排列共耗时："+(currentTime-start)/1000+"秒");start=currentTime;
        /** 第三步：向下分发最优解（依据最优解，调用子域节点的receiveCacheRequest方法）  **/
        Map<Integer,LinkedList<CacheOrderRequest>> result=new HashMap<>();
        c4(result,resultHomogenization);
        c3(result,resultHeterogenization);
        for(Map.Entry<Integer,LinkedList<CacheOrderRequest>> entry:result.entrySet()){
            EdgeServer edgeServer=router.getEdgeServer(entry.getKey());
            edgeServer.receiveCacheRequest(entry.getValue(),router);
        }
        currentTime=System.currentTimeMillis();
        System.out.println("策略分发："+(currentTime-start)/1000+"秒");
    }


    /**
     * 回溯算法，返回的最终结果是，内容分配到
     * @param needDistributeHeterogenization 待分配的同质化缓存
     * @param index 当前迭代过程的内容索引（needDistributeHomogenization下）
     * @param path 当前回溯路径
     * @param resultHeterogenization 当前最优解
     * @param selector 候选节点
     * @param router 路由信息
     * @param records_from_count 内容来源统计
     */
    private void dfs_Heterogenization(HashMap<String,HashMap<Integer,Integer>> records_from_count
            , String[] needDistributeHeterogenization, int index, HashMap<String,Integer> path,
                                      LinkedList<Integer> selector, Router router){
        if(path.size()== needDistributeHeterogenization.length){
            //说明遍历到子节点了
            if(resultHeterogenization==null){
                resultHeterogenization=new DfsResultHeterogenization();
                resultHeterogenization.ret=new HashMap<>(path);
                resultHeterogenization.current=this.computeHeterogenization(records_from_count,path,router);
            }else{
                long al=this.computeHeterogenization(records_from_count,path,router);
                if(resultHeterogenization.current>al){
                    //当前才是最优解
                    resultHeterogenization.current=al;
                    resultHeterogenization.ret=new HashMap<>(path);
                }
            }
            return;
        }
        for(int select:selector){
            path.put(needDistributeHeterogenization[index],select);
            dfs_Heterogenization(records_from_count,needDistributeHeterogenization,index+1,path,selector,router);
            path.remove(needDistributeHeterogenization[index]);
        }
    }
    /**
     * 回溯计算同质化缓存最优解决方案
     * @param resultHomogenization 返回的结果
     * @param needDistributeHomogenization 待分配的同质化缓存
     * @param index 当前迭代过程的内容索引（needDistributeHomogenization）（即需要为当内容指定缓存方案）
     * @param path 当前回溯路径
     * @param resultHomogenization 当前最优解
     * @param selector 候选节点
     * @param router 路由信息
     * @param records_from_count 内容来源统计
     */
    private void dfs_Homogenization(HashMap<String,HashMap<Integer,Integer>> records_from_count
            ,String[] needDistributeHomogenization, int index
            , HashMap<String,LinkedList<Integer>> path, LinkedList<LinkedList<Integer>> selector, Router router){
        if(path.size()== needDistributeHomogenization.length){
            if(resultHomogenization==null){
                resultHomogenization=new DfsResultHomogenization();
                resultHomogenization.ret=new HashMap<>(path);
                resultHomogenization.current=this.computeHomogenization(records_from_count,path,router);
            }else{
                long t=this.computeHomogenization(records_from_count,path,router);
                if(resultHomogenization.current==t){
                    //选择占用空间小的
                    int currentOccupy=0;
                    for(Map.Entry<String,LinkedList<Integer>> entry:path.entrySet()){
                        currentOccupy+=entry.getValue().size();
                    }
                    int lastOccupy=0;
                    for(Map.Entry<String,LinkedList<Integer>> entry:resultHomogenization.ret.entrySet()){
                        lastOccupy+=entry.getValue().size();
                    }
                    if(currentOccupy<lastOccupy){
                        resultHomogenization.ret=new HashMap<>(path);
                    }
                }else if(resultHomogenization.current>t){
                    resultHomogenization.ret=new HashMap<>(path);
                    resultHomogenization.current=t;
                }
                //相等了就遍历到末尾了，return
            }
            return;
        }
        for(LinkedList<Integer> selected:selector){
            path.put(needDistributeHomogenization[index],selected);
            dfs_Homogenization(records_from_count,needDistributeHomogenization,index+1,path,selector,router);
            path.remove(needDistributeHomogenization[index]);
        }
    }
    /**
     * 计算全排列，同质化缓存中，每个缓存内容可以缓存的全部位置方案
     * @param dfs_fullArrayRet 结果
     * @param selector 候选缓存节点
     * @param path 当前选择的缓存节点
     * @param alreadyUse 已经使用的缓存节点（防止同节点重复缓存）
     */
    private void dfs_fullArray(LinkedList<Integer> selector,LinkedList<Integer> path,HashSet<Integer> alreadyUse){
        if(path.size()>0) {
            dfs_fullArrayRet.add(new LinkedList<>(path));
        }
        for(int selected:selector){
            if(path.size()==0||(path.getLast()<selected)&&(!alreadyUse.contains(selected))){
                path.add(selected);
                alreadyUse.add(selected);
                dfs_fullArray(selector,path,alreadyUse);
                path.removeLast();
                alreadyUse.remove(selected);
            }
        }
    }


    /**
     * 计算同质化当前解决方案的总访问跳数，从每个用户源出发找到所有缓存内容的最短路径（每个内容缓存位置实际上是一个集合）
     * @param records_from_count 记录来源统计
     * @param path 解决方案
     * @param router 全局路由
     * @return 总跳数
     */
    private long computeHomogenization(HashMap<String,HashMap<Integer,Integer>> records_from_count
            ,HashMap<String,LinkedList<Integer>> path,Router router){
        long ret=0;
        for(Map.Entry<String,LinkedList<Integer>> entry1:path.entrySet()){
            String v=entry1.getKey();
            LinkedList<Integer> list=entry1.getValue();
            //int seq=entry1.getValue();
            //m1表示内容v来源统计
            HashMap<Integer,Integer> m1=records_from_count.get(v);
            for(Map.Entry<Integer,Integer> entry2:m1.entrySet()){
                int from=entry2.getKey();
                long count=entry2.getValue();
                //找到最短路径
                int distance=999;
                int path_length_from_core=router.getPathLength(from,router.core.seq,router.plNoPeer);
                for(int seq:list){
                    //找到所有缓存位置中的最短路径
                    distance=Math.min(distance,router.getPathLength(seq,from, router.plNoPeer));
                }
                //需要与核心服务器的距离比较，取较小值
                distance=Math.min(distance,path_length_from_core);
                ret+=count*distance;
            }
        }
        return ret;
    }

    /**
     * 计算异质化解决方案的总访问跳数
     * @param records_from_count 记录来源统计
     * @param path 解决方案
     * @param router 全局路由
     * @return 总跳数
     */
    private long computeHeterogenization(HashMap<String,HashMap<Integer,Integer>> records_from_count
            ,HashMap<String,Integer> path,Router router){
        long ret=0;
        for(Map.Entry<String,Integer> entry1:path.entrySet()){
            String v=entry1.getKey();
            int seq=entry1.getValue();
            //m1表示内容v来源统计
            HashMap<Integer,Integer> m1=records_from_count.get(v);
            for(Map.Entry<Integer,Integer> entry2:m1.entrySet()){
                int from=entry2.getKey();
                long count=entry2.getValue();
                int distance=Math.min(router.getPathLength(from,router.core.seq,router.plNoPeer),router.getPathLength(seq,from, router.plNoPeer));
                ret+=count*distance;
            }
        }
        return ret;
    }
    /** 不需要，因为缓存下发过程中会触发缓存替换算法，所以在决定解决方案是可以不考虑子节点的状态，单纯以最佳路由长度作为决策依据
     * 判断是否可以将内容v缓存到节点edgeServer上
     * @param v 内容
     * @param edgeServer 节点
     * @param path 当前选择路径（路径没有实际占用缓存空间，但是也会在当前解决方案中占用缓存空间）
     * @return 返回是否可以
     */
    private boolean canAdd(String v,EdgeServer edgeServer,HashMap<String,Integer> path){
        return true;
    }

    public EdgeServer(int seq, int level) {
        this.seq = seq;
        this.level = level;
    }

    public EdgeServer(){

    }
    public int seq;
    /**
     * 0 核心层、1 汇聚层、2 边缘层   3 通用层
     */
    public int level;

    /**
     * 因为允许同质化缓存，所以子域内可能存储多个相同内容给的缓存
     * 每个节点的DHT表仅存储本节点子域内的缓存情况（向下）
     */
    HashMap<String, HashSet<Integer>> DHT = new HashMap<>();

    /**
     * 父节点可以接受子域子节点的缓存更新信息 , 在子节点收到缓存请求时更新本地缓存结束后，更新父节点的DHT信息
     * @param v 缓存集合
     * @param seq 下游节点序号
     */
    public void receiveAndUpdateDHT(HashSet<String> v ,int seq){
        HashMap<String,HashSet<Integer>> needRemoveRoot=new HashMap<>();
        //除了添加子节点的缓存信息，还要将子节点删除的缓存同时在dht中删除，先删除再添加的目的是为了减少循环次数
        for(String s:DHT.keySet()){
            if(!v.contains(s)){
                if(DHT.get(s).contains(seq)){
                    //seq节点发来的内容没有包括s，但是s原来是seq的缓存之一，所以要将s的缓存删除seq
                    if(needRemoveRoot.containsKey(s)){
                        needRemoveRoot.get(s).add(seq);
                    }else{
                        HashSet<Integer> set=new HashSet<>();
                        set.add(seq);
                        needRemoveRoot.put(s,set);
                    }
                }
            }
        }
        //删除链中节点
        for(Map.Entry<String,HashSet<Integer>> entry1:needRemoveRoot.entrySet()){
            for(int i:entry1.getValue()){
                this.DHT.get(entry1.getKey()).remove(i);
            }
        }
        LinkedList<String> needRemoveKey=new LinkedList<>();
        for(Map.Entry<String,HashSet<Integer>> entry:DHT.entrySet()){
            if(entry.getValue().size()==0){
                needRemoveKey.add(entry.getKey());
            }
        }
        //真正删除
        for(String key:needRemoveKey){
            DHT.remove(key);
        }


        for(String s:v){
            if(this.DHT.containsKey(s)){
                this.DHT.get(s).add(seq);
            }else{
                HashSet<Integer> set=new HashSet<>();
                set.add(seq);
                this.DHT.put(s,set);
            }
        }

    }

    private int getFirstSeqByDHTString(String v){
        return DHT.get(v).iterator().next();
    }
    /**
     * 缓存容量
     */
    public int caption = 10;

    //如果是汇聚层或者边缘层，下游节点集合
    public LinkedList<Integer> children = new LinkedList<>();
    //上游节点
    public int parent;

    //偏好,仅针对用户终端使用
    public char gap;

    //无条件添加缓存内容
    public void Force_Add(String cont,int layer) {
        if(this.cache.containsKey(cont)){
            this.cache.get(cont).add(layer);
        }else{
            HashSet<Integer> set=new HashSet<>();
            set.add(layer);
            this.cache.put(cont,set);
        }
    }



    //每个边缘服务器基础动作 1、收到请求  2、转发请求 3、收到响应 4、转发响应
    //缓存动作 1、询问下游子节点  2、做出决策 3、缓存
    /**
     * 收到请求，并给出响应。是否缓存则在收到响应之后决定
     * 收到请求一共有3条路
     * 1、本地有缓存，直接满足响应。（直接生成响应向下发送）
     * 2、本地没有缓存，但是有联合缓存。生成新的请求，起点为当前节点，终点为有数据的缓存点。然后递归
     * 3、本地没有缓存，且没有联合缓存，向上转发请求。
     *
     * 内容热度表更新分2条路走，（防止重复计算） 只有来自于终端的请求才会更新热度表
     * 1、请求到达节点时，立即更新本地hotTable表
     * 2、若当前节点能满足请求，则需要递归通知当前节点父节点更新hotTable表
     */
    public Response receiveRequest(Request request, Router router){
        request.hop++;
        request.current=this.seq;

        //记录下来每一次内容请求到达本节点的路由长度
        if(request.isFromUser(router)) {
            this.updateRecords(request.value,request.path.getFirst());
        }
        if(this.cache.containsKey(request.value)){
            //本节点已经满足缓存，直接返回。创建新的响应，并向下发送
            Response response=new Response(this.seq,request.path.getFirst(),request.value, router.plNoPeer);
            response.hop_request=request.getHop();
            return router.getEdgeServer(response.next()).reciveResponse(response,router);
        }
        if(DHT.containsKey(request.value)){
            //DHT仅包含子域的缓存，所以若DHT有缓存，直接返回第一个，就好，不管怎么样路由一定是1
            Request myReq=new Request(this.seq,getFirstSeqByDHTString(request.value),request.value,router.plNoPeer);
            Response respTemp=router.getEdgeServer(request.next()).receiveRequest(myReq,router);
            //收到响应后，生成响应给用户的响应
            Response response=new Response(this.seq,request.path.getFirst(),request.value, router.plNoPeer,respTemp.hop);
            response.hop_request=request.getHop()+respTemp.hop;
            return router.getEdgeServer(response.next()).reciveResponse(response,router);
        }
        return router.getEdgeServer(request.next()).receiveRequest(request,router);
    }

    /**
     * 节点收到响应实际上只有2件事
     * 1、继续向链路下游转发响应 （响应也有跳数）
     * 2、决定是否缓存响应内容，以及缓存在什么位置
     * 缓存涉及到：
     * 是否缓存：高层节点不可以同质化缓存
     * 1、查看本节点是否有足够的空间，若有足够空间，且未被缓存，则直接缓存。（命中缓存时，需要将响应中是否已缓存设置为true）
     * 2、本节点没有足够缓存空间时：
     *  2.1 根据lastTimeTable和hotTable来计算缓存收益，看是否替换本节点缓存内容
     *  2.2 若能替换，创建一个缓存请求，向下游发出缓存请求，得到缓存收益得分最高的节点。
     *
     *  统计所有路由
     *  内容流行度越高、内容最后一次访问时间越近、被替换出去的缓存收益得分越低、全量（经过该节点，能使得全局访问内容的平均跳数最低），那么缓存收益得分越高
     *  缓存收益得分影响因子
     *  由A缓存到B，那么A所有下游节点要想访问内容，都必须经过A到B拿内容。A将内容发送到B的同时，还会把自己下游所有对A请求的所有路径长度发送给B（不包含B自己下游）
     *
     * 没有缓存时，访问A的代价是多少，缓存到本节点的，访问A的代价是多少，代价做差就是收益增值。
     * 但是发生替换时，为了缓存A而替换缓存B，缓存了B和没有缓存B的代价之差是收益减值。所以要计算真实缓存收益，B的缓存收益，默认变为访问核心
     *
     * 缓存替换
     * 1、缓存替换需要综合考虑到内容流行度以及内容访问排名
     *
     * 缓存调整
     */
    public Response reciveResponse(Response response,Router router){
        response.hop++;
        response.current=this.seq;
        if(this.seq==response.path.getLast()){
            //响应已经到达目的地
            return response;
        }
        return router.getEdgeServer(response.next()).reciveResponse(response,router);
    }


    /**
     * 更新节点访问表（只有来源于用户才记录，否则大量转发会导致缓存固化）
     * @param v
     * @param seq
     */
    public void updateRecords(String v,int seq){
        this.records.add(new Record(v,System.currentTimeMillis(),seq));
    }

}
