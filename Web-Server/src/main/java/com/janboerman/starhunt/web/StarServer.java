package com.janboerman.starhunt.web;

import org.eclipse.jetty.server.Server;

import java.util.logging.Logger;

public class StarServer {

    private StarServer() {
    }

    public static void main(String[] args) throws Exception {
        Logger logger = Logger.getLogger("star server");

        //TODO get port number from option command line arguments (use an OptionParser)
        //TODO otherwise fall back to 8080
        final int port = 8080;

        //TODO set HTTPS (SSL) https://dzone.com/articles/adding-ssl-support-embedded but use LetsEncrypt instead.

        Server server = new Server(port);
        server.setHandler(new StarHandler(new StarDatabase()));

        logger.info("Started StarServer on port " + port + "!");

        server.start();
        server.join();
    }


}
