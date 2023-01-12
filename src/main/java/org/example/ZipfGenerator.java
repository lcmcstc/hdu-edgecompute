package org.example;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

public class ZipfGenerator {
    private final Random random = new Random(0);
    public LinkedList<Content> contents;

    public HashMap<Character,LinkedList<Content>> gap_contents;
    /**
     * 偏好区间，暂时按照固定百分比，分为4个区间，40% 30% 20% 10%
     * 先根据偏好选定内容区间，再在内容区间内按照齐普夫分布进行结合。
     */
    private static final  double Constant = 1.0;

    public char[] gaps=new char[]{'A','B','C','D'};
    public ZipfGenerator(int size, double skew)
    {
        contents=new LinkedList<>();
        gap_contents=new HashMap<>();
        for(char c:gaps){
            double lastSumRatio=0;
            LinkedList<Content> cts=computeMap(size, skew);
            for(Content content:cts){
                content.val=c+"_"+content.val;
                content.lastSumRatio=lastSumRatio;
                lastSumRatio=lastSumRatio+content.ratio;
            }
            gap_contents.put(c,cts);
            contents.addAll(cts);
        }
    }
    //size为rank个数，skew为数据倾斜程度, 取值为0表示数据无倾斜，取值越大倾斜程度越高
    private static LinkedList<Content> computeMap(
            int size, double skew){
        LinkedList<Content> ret = new LinkedList<Content>();
        //总频率
        double div = 0;
        //对每个rank，计算对应的词频，计算总词频
        for (int i = 1; i <= size; i++)
        {
            //the frequency in position i
            div += (Constant / Math.pow(i, skew));
        }
        //计算每个rank对应的y值，所以靠前rank的y值区间远比后面rank的y值区间大
        double sum = 0;
        for (int i = 1; i <= size; i++)
        {
            double p = (Constant / Math.pow(i, skew)) / div;
            sum += p;
            //if ((i * 1.0) / (size * 1.0) >= 0.2) {
            //    int sa = 1;
            //}
            ret.add(new Content(p, String.valueOf(i)));
            //map.put(sum, i - 1);
        }
        return ret;
    }

    public Content getRandomContentByGap(char gap){
        double r=random.nextDouble();
        for(Content content:this.gap_contents.get(gap)){
            if(content.lastSumRatio+content.ratio>=r){
                return content;
            }
        }
        return new Content(0,"没有命中");
    }
}
