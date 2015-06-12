package com.dhgate.ttorrent.cli;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.curator.retry.BoundedExponentialBackoffRetry;

/**
 * Created by Administrator on 15-6-12.
 */
abstract public class TtorrentUpdateProcess {

    static String connectString = "localhost";
    static int connectionTimeoutMs = 15000;
    static int sessionTimeoutMs = 30000;
    static CuratorFramework zkClient;
    static String zkDataPath = "/seed";

    static {
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                .connectString(connectString).connectionTimeoutMs(connectionTimeoutMs)
                .sessionTimeoutMs(sessionTimeoutMs).retryPolicy(new BoundedExponentialBackoffRetry(3 * 1000, 60 * 1000, 15));
        zkClient = builder.build();
        zkClient.start();
    }

    protected void register(){
        final NodeCache nodeCache = new NodeCache(zkClient, zkDataPath);
        nodeCache.getListenable().addListener(new NodeCacheListener() {
            @Override
            public void nodeChanged() throws Exception {
                ChildData currentData = nodeCache.getCurrentData();
                if (currentData != null) {
                    byte[] data = currentData.getData();
                    if (data != null) {
                        String status = new String(data);
                    }
                }
            }
        });

        try {
            nodeCache.start();
            System.out.println("Start Status Listeners , succssed... ");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void upload(byte[] data){
        try {
            zkClient.create().forPath(zkDataPath,data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    abstract void processData(byte[] data);

}
