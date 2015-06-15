/**
 * Copyright (C) 2011-2013 Turn, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dhgate.ttorrent.cli;

import com.google.common.collect.Sets;
import com.turn.ttorrent.client.Client;
import com.turn.ttorrent.client.SharedTorrent;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.channels.UnsupportedAddressTypeException;
import java.util.*;

/**
 * Command-line entry-point for starting a {@link com.turn.ttorrent.client.Client}
 */
public class TorrentSlave extends TorrentUpdateProcess {

	private static final Logger logger =
		LoggerFactory.getLogger(TorrentSlave.class);

	/**
	 * Default data output directory.
	 */
	private static final String DEFAULT_OUTPUT_DIRECTORY = "/tmp";

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
	 * Main client entry point for stand-alone operation.
	 */
	public static void main(String[] args) throws InterruptedException {
        ConsoleAppender consoleAppender = new ConsoleAppender(
                new PatternLayout("%d [%-25t] %-5p: %m%n"));
        consoleAppender.setThreshold(Priority.INFO);
        BasicConfigurator.configure(consoleAppender);

        TorrentSlave torrentSlave = new TorrentSlave();
        torrentSlave.register();
        while(true){
            Thread.sleep(1);
        }
	}

    Stack<Client> cs = new Stack<Client>();
    String slavePath = "d:/tmp/torrent/slave";

    /***
     * 获取指定目录下的所有的文件（包括文件夹）
     *
     * @param obj
     * @return
     */
    public static List<File> getListFiles(Object obj) {
        File directory = null;
        if (obj instanceof File) {
            directory = (File) obj;
        } else {
            directory = new File(obj.toString());
        }
        List<File> files = new ArrayList<File>();
        if (directory.isFile()) {
            files.add(directory);
            return files;
        } else if (directory.isDirectory()) {
            File[] fileArr = directory.listFiles();
            for (int i = 0; i < fileArr.length; i++) {
                File fileOne = fileArr[i];
                files.addAll(getListFiles(fileOne));
            }
            files.add(directory);
        }
        return files;
    }

    /**
     * 停止已经存在的
     * @param data
     */
    @Override
    void processData(byte[] data) {
        logger.info("Slave got data length: " + data.length);

        try {
            while(!cs.empty()){
                cs.pop().stop();
            }
            File f = new File(slavePath);
            SharedTorrent sharedTorrent = new SharedTorrent(data, f);



            Client c = new Client(
                    getIPv4Address(null),
                    sharedTorrent);
            cs.push(c);
            c.setMaxDownloadRate(0);
            c.download();
            c.waitForCompletion();

            List<String> filenames = sharedTorrent.getFilenames();
            Set<String> newFiles = Sets.newHashSet();
            for (int i = 0; i < filenames.size(); i++) {
                String fp = new File(slavePath, filenames.get(i)).getAbsolutePath();
                logger.info("torrent: " + fp + " " + fp.hashCode());
                newFiles.add(fp);
            }
            List<File> fileList = getListFiles(f);
            for (int i = 0; i < fileList.size(); i++) {
                String fp = fileList.get(i).getAbsolutePath();
                if(!newFiles.contains(fp)){
                    logger.info("remove: " + fp + " " + fp.hashCode());
                    fileList.get(i).delete();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
