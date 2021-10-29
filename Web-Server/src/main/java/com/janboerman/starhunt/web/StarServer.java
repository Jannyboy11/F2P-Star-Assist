package com.janboerman.starhunt.web;

import org.eclipse.jetty.server.Server;

import java.util.logging.Logger;

public class StarServer {

    private StarServer() {
    }

    public static void main(String[] args) throws Exception {
        Logger logger = Logger.getLogger("star server");

        final int port = 8080;

        Server server = new Server(port);
        server.setHandler(new StarHandler(new StarDatabase()));

        logger.info("Started StarServer on port " + port + "!");

        server.start();
        server.join();
    }


}
