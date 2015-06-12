package com.dhgate.ttorrent.cli;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.curator.retry.BoundedExponentialBackoffRetry;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Administrator on 15-6-12.
 */
abstract public class TtorrentUpdateProcess {
    private static final Logger logger =
            LoggerFactory.getLogger(TtorrentUpdateProcess.class);
    static String connectString = "localhost";
    static int connectionTimeoutMs = 15000;
    static int sessionTimeoutMs = 30000;
    static CuratorFramework zkClient;
    static String zkDataPath = "/seed";

    public TtorrentUpdateProcess(){
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
                        processData(data);
                    }
                }
            }
        });

        try {
            nodeCache.start();
            logger.info("Start Status Listeners , succssed... ");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void upload(byte[] data){
        try {
//            Stat stat = zkClient.checkExists().forPath(zkDataPath);
            zkClient.newNamespaceAwareEnsurePath(zkDataPath);
            zkClient.setData().forPath(zkDataPath,data);
            logger.info("upload success");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    abstract void processData(byte[] data);

}
