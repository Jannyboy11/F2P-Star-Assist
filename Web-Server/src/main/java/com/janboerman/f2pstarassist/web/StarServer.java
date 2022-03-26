package com.janboerman.f2pstarassist.web;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.eclipse.jetty.server.Server;

import java.util.logging.Level;
import java.util.logging.Logger;

public class StarServer {

    private StarServer() {
    }

    public static void main(String[] args) throws Exception {
        final Logger logger = Logger.getLogger("star server");

        final OptionParser optionParser = new OptionParser();
        final OptionSpec<Integer> portSpec = optionParser.accepts("port", "port number on which to run the web server")
                .withRequiredArg()
                .withValuesConvertedBy(new PortConverter())
                .defaultsTo(8080);
        //TODO option to configure the file used for the unix domain socket for communication with the discord bot
        final OptionSet options = optionParser.parse(args);

        final int port = options.valueOf(portSpec);

        final Server server = new Server(port);
        final StarDatabase starDatabase = new StarDatabase(NoOpStarListener.INSTANCE);
        server.setHandler(new StarHandler(starDatabase, logger));

        //TODO set HTTPS (SSL) https://dzone.com/articles/adding-ssl-support-embedded but use LetsEncrypt instead.

        logger.info("Started StarServer on port " + port + "!");

        server.start();
        server.join();
    }


}
