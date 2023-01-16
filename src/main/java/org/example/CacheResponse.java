package org.example;

public class CacheResponse {
    public boolean canCache;

    public static CacheResponse cantCache(){
        CacheResponse cacheResponse=new CacheResponse();
        cacheResponse.canCache=false;
        return cacheResponse;
    }
}
