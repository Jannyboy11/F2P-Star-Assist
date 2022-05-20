package com.janboerman.f2pstarassist.web;

import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.util.logging.Logger;

public class StarServer {

    private final Configuration config;
    private final Logger logger;

    public StarServer(Configuration config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    public void start() throws Exception {

        final Server server = new Server();
        final ServerConnector serverConnector;

        final int port = config.port();
        if (config.ssl()) {
            HttpConfiguration https = new HttpConfiguration();
            https.addCustomizer(new SecureRequestCustomizer());

            SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
            sslContextFactory.setKeyStorePath(config.keyStore().toString());
            sslContextFactory.setKeyStorePassword(config.keyStorePassword());
            sslContextFactory.setKeyManagerPassword(config.keyStorePassword());

            SslConnectionFactory sslConnectionFactory = new SslConnectionFactory(sslContextFactory, "http/1.1");

            serverConnector = new ServerConnector(server, sslConnectionFactory, new HttpConnectionFactory(https));
        } else {
            serverConnector = new ServerConnector(server);
        }
        serverConnector.setPort(port);
        server.setConnectors(new ServerConnector[] {serverConnector});

        final StarDatabase starDatabase = new StarDatabase(NoOpStarListener.INSTANCE);
        server.setHandler(new StarHandler(starDatabase, logger));

        logger.info("Started StarServer on port " + port + "!");

        server.start();
        server.join();
    }

}
