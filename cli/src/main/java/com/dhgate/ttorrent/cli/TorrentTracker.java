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

import com.turn.ttorrent.tracker.TrackedTorrent;
import com.turn.ttorrent.tracker.Tracker;
import jargs.gnu.CmdLineParser;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;

/**
 * Command-line entry-point for starting a {@link com.turn.ttorrent.tracker.Tracker}
 */
public class TorrentTracker extends TorrentUpdateProcess {

    private static final Logger logger =
            LoggerFactory.getLogger(TorrentTracker.class);

    private Tracker t;

    public TorrentTracker(int port){
        super();
        try {
            t = new Tracker(new InetSocketAddress(port));
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
            throw new RuntimeException("TorrentTracker init error", e);
        }
        register();
        logger.info("Starting tracker with {} announced torrents...",
                t.getTrackedTorrents().size());
        t.start();
    }


    /**
     * Display program usage on the given {@link java.io.PrintStream}.
     */
    private static void usage(PrintStream s) {
        s.println("usage: Tracker [options] [directory]");
        s.println();
        s.println("Available options:");
        s.println("  -h,--help             Show this help and exit.");
        s.println("  -p,--port PORT        Bind to port PORT.");
        s.println();
    }


    /**
     *
     * @param torrent
     */
    @Override
    void processData(byte[] torrent) {
        System.out.println("Tracker got torrent");
        try {
            if (torrent != null && torrent.length > 0)
                t.announce(new TrackedTorrent(torrent));
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
            e.printStackTrace();
        }
    }

    /**
     * Main function to start a tracker.
     */
    public static void main(String[] args) throws IOException {
        ConsoleAppender consoleAppender = new ConsoleAppender(
                new PatternLayout("%d [%-25t] %-5p: %m%n"));
        consoleAppender.setThreshold(Priority.INFO);
        BasicConfigurator.configure(consoleAppender);

        CmdLineParser parser = new CmdLineParser();
        CmdLineParser.Option help = parser.addBooleanOption('h', "help");
        CmdLineParser.Option port = parser.addIntegerOption('p', "port");

        try {
            parser.parse(args);
        } catch (CmdLineParser.OptionException oe) {
            System.err.println(oe.getMessage());
            usage(System.err);
            System.exit(1);
        }

        // Display help and exit if requested
        if (Boolean.TRUE.equals((Boolean) parser.getOptionValue(help))) {
            usage(System.out);
            System.exit(0);
        }

        Integer portValue = (Integer) parser.getOptionValue(port,
                Integer.valueOf(Tracker.DEFAULT_TRACKER_PORT));

        new TorrentTracker(portValue);
    }


}
