package org.example;

import java.util.LinkedList;

public class PathLength {

    public int[][] pic;
    public int[][] dp;
    public int[][] midPoint;

    public LinkedList[][] paths;

    public LinkedList<Integer> getShortestPath(int start,int end){
        return paths[start][end];
    }
}
