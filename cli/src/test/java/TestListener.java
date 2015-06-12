import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.curator.retry.BoundedExponentialBackoffRetry;
import sun.rmi.runtime.Log;

/**
 * Created by Administrator on 15-6-12.
 */
public class TestListener {

    private void listenSeed(){
        String connectString = "localhost";
        int connectionTimeoutMs = 15000;
        int sessionTimeoutMs = 30000;
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                .connectString(connectString).connectionTimeoutMs(connectionTimeoutMs)
                .sessionTimeoutMs(sessionTimeoutMs).retryPolicy(new BoundedExponentialBackoffRetry(3 * 1000, 60 * 1000, 15));
        CuratorFramework zkClient = builder.build();
        final NodeCache nodeCache = new NodeCache(zkClient, "seed");
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


    public static void main(String[] args) {

    }


}
