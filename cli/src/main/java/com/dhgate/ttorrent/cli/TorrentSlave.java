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
import java.io.PrintStream;
import java.net.*;
import java.nio.channels.UnsupportedAddressTypeException;
import java.util.Enumeration;
import java.util.List;
import java.util.Stack;

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

    Stack<Client> cs = new Stack<Client>();

    /**
     *
     * @param data
     */
    @Override
    void processData(byte[] data) {
        logger.info("Slave got data length: " + data.length);

        try {
            while(!cs.empty()){
                cs.pop().stop();
            }
            SharedTorrent sharedTorrent = new SharedTorrent(data, new File("d:/tmp/torrent/slave"));
            //TODO:删除多余的文件
            List<String> filenames = sharedTorrent.getFilenames();

            Client c = new Client(
                    getIPv4Address(null),
                    sharedTorrent);
            cs.push(c);
            c.setMaxDownloadRate(0);
            c.download();
        } catch (IOException e) {
            e.printStackTrace();
        }
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


}
