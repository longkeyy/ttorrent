package com.dhgate.ttorrent.cli;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.curator.retry.BoundedExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.nio.channels.UnsupportedAddressTypeException;
import java.util.Enumeration;

/**
 * Created by Administrator on 15-6-12.
 */
abstract public class TorrentUpdateProcess {
    private static final Logger logger =
            LoggerFactory.getLogger(TorrentUpdateProcess.class);
    static String connectString = "localhost";
    static int connectionTimeoutMs = 15000;
    static int sessionTimeoutMs = 30000;
    static CuratorFramework zkClient;
    static String zkDataPath = "/seed";

    public TorrentUpdateProcess(){
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                .connectString(connectString).connectionTimeoutMs(connectionTimeoutMs)
                .sessionTimeoutMs(sessionTimeoutMs).retryPolicy(new BoundedExponentialBackoffRetry(3 * 1000, 60 * 1000, 15));
        zkClient = builder.build();
        zkClient.start();
    }

    /**
     * Returns a usable {@link java.net.Inet4Address} for the given interface name.
     *
     * <p>
     * If an interface name is given, return the first usable IPv4 address for
     * that interface. If no interface name is given or if that interface
     * doesn't have an IPv4 address, return's localhost address (if IPv4).
     * </p>
     *
     * <p>
     * It is understood this makes the client IPv4 only, but it is important to
     * remember that most BitTorrent extensions (like compact peer lists from
     * trackers and UDP tracker support) are IPv4-only anyway.
     * </p>
     *
     * @param iface The network interface name.
     * @return A usable IPv4 address as a {@link java.net.Inet4Address}.
     * @throws java.nio.channels.UnsupportedAddressTypeException If no IPv4 address was available
     * to bind on.
     */
    public static Inet4Address getIPv4Address(String iface)
            throws SocketException, UnsupportedAddressTypeException,
            UnknownHostException {
        if (iface != null) {
            Enumeration<InetAddress> addresses =
                    NetworkInterface.getByName(iface).getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress addr = addresses.nextElement();
                if (addr instanceof Inet4Address) {
                    return (Inet4Address)addr;
                }
            }
        }

        InetAddress localhost = InetAddress.getLocalHost();
        if (localhost instanceof Inet4Address) {
            return (Inet4Address)localhost;
        }

        throw new UnsupportedAddressTypeException();
    }

    /**
     *
     */
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

    /**
     *
     * @param data
     */
    public void upload(byte[] data){
        try {
            zkClient.newNamespaceAwareEnsurePath(zkDataPath);
            zkClient.setData().forPath(zkDataPath,data);
            logger.info("upload success");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    abstract void processData(byte[] data);

}
