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
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;
import com.turn.ttorrent.client.Client;
import com.turn.ttorrent.client.SharedTorrent;
import com.turn.ttorrent.common.Torrent;
import jargs.gnu.CmdLineParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.PrintStream;
import java.net.*;
import java.nio.channels.UnsupportedAddressTypeException;
import java.util.*;

/**
 * Command-line entry-point for starting a {@link com.turn.ttorrent.client.Client}
 */
public class TorrentMaster extends TorrentUpdateProcess {

	private static final Logger logger =
		LoggerFactory.getLogger(TorrentMaster.class);

	/**
	 * Default data output directory.
	 */
	private static final String DEFAULT_OUTPUT_DIRECTORY = "/tmp";


    public byte[] create(Set<String> announceURLs, int pieceLengthVal, String sharePath){
        ByteOutputStream fos = null;
        byte[] data = new byte[0];
        try {

                fos = new ByteOutputStream();
                //Process the announce URLs into URIs
                List<URI> announceURIs = new ArrayList<URI>();
                for (String url : announceURLs) {
                    announceURIs.add(new URI(url));
                }

                //Create the announce-list as a list of lists of URIs
                //Assume all the URI's are first tier trackers
                List<List<URI>> announceList = new ArrayList<List<URI>>();
                announceList.add(announceURIs);

                File source = new File(sharePath);
                if (!source.exists() || !source.canRead()) {
                    throw new IllegalArgumentException(
                            "Cannot access source file or directory " +
                                    source.getName());
                }

                String creator = String.format("%s (ttorrent)",
                        System.getProperty("user.name"));

                Torrent torrent = null;
                if (source.isDirectory()) {
                    List<File> files = new ArrayList<File>(FileUtils.listFiles(source, TrueFileFilter.TRUE, TrueFileFilter.TRUE));
                    Collections.sort(files);
                    torrent = Torrent.create(source, files, pieceLengthVal,
                            announceList, creator);
                } else {
                    torrent = Torrent.create(source, pieceLengthVal, announceList, creator);
                }
                torrent.save(fos);
                data= fos.getBytes();
        } catch (Exception e) {
            logger.error("{}", e.getMessage(), e);
            System.exit(2);
        } finally {
                IOUtils.closeQuietly(fos);
        }
        return data;
    }

	/**
	 * Main client entry point for stand-alone operation.
	 */
	public static void main(String[] args) throws InterruptedException {
        ConsoleAppender consoleAppender = new ConsoleAppender(
                new PatternLayout("%d [%-25t] %-5p: %m%n"));
        consoleAppender.setThreshold(Priority.INFO);
        BasicConfigurator.configure(consoleAppender);

		CmdLineParser parser = new CmdLineParser();
		CmdLineParser.Option help = parser.addBooleanOption('h', "help");
		CmdLineParser.Option output = parser.addStringOption('o', "output");
		CmdLineParser.Option iface = parser.addStringOption('i', "iface");
		CmdLineParser.Option seedTime = parser.addIntegerOption('s', "seed");
		CmdLineParser.Option maxUpload = parser.addDoubleOption('u', "max-upload");
		CmdLineParser.Option maxDownload = parser.addDoubleOption('d', "max-download");

//		try {
//			parser.parse(args);
//		} catch (CmdLineParser.OptionException oe) {
//			System.err.println(oe.getMessage());
//			usage(System.err);
//			System.exit(1);
//		}
//
//		// Display help and exit if requested
//		if (Boolean.TRUE.equals((Boolean)parser.getOptionValue(help))) {
//			usage(System.out);
//			System.exit(0);
//		}
//
//		String outputValue = (String)parser.getOptionValue(output,
//			DEFAULT_OUTPUT_DIRECTORY);
//		String ifaceValue = (String)parser.getOptionValue(iface);
//		int seedTimeValue = (Integer)parser.getOptionValue(seedTime, -1);
//
//		double maxDownloadRate = (Double)parser.getOptionValue(maxDownload, 0.0);
//		double maxUploadRate = (Double)parser.getOptionValue(maxUpload, 0.0);
//
//		String[] otherArgs = parser.getRemainingArgs();
//		if (otherArgs.length != 1) {
//			usage(System.err);
//			System.exit(1);
//		}

        TorrentMaster torrentMain = new TorrentMaster();
        torrentMain.register();
        Set<String> announceURLs = Sets.newHashSet();
        announceURLs.add("http://192.168.54.34:6969/announce");
        String sharePath = "d:/tmp/torrent/master/ai";
        String shardParent = "d:/tmp/torrent/master";
        byte[] data = torrentMain.create(announceURLs, Torrent.DEFAULT_PIECE_LENGTH, sharePath);
        torrentMain.upload(data);
        try {
            String ifaceValue = null;
            Client c = new Client(
				getIPv4Address(ifaceValue),
                    new SharedTorrent(data, new File(shardParent)));

//			c.setMaxDownloadRate(maxDownloadRate);
//			c.setMaxUploadRate(maxUploadRate);

			// Set a shutdown hook that will stop the sharing/seeding and send
			// a STOPPED announce request.
			Runtime.getRuntime().addShutdownHook(
				new Thread(new Client.ClientShutdown(c, null)));

            int seedTimeValue = Integer.MAX_VALUE;
            c.share(seedTimeValue);
			if (Client.ClientState.ERROR.equals(c.getState())) {
				System.exit(1);
			}
		} catch (Exception e) {
			logger.error("Fatal error: {}", e.getMessage(), e);
			System.exit(2);
		}

        while(true){
            Thread.sleep(1);
        }
	}

    @Override
    void processData(byte[] data) {

    }
}
