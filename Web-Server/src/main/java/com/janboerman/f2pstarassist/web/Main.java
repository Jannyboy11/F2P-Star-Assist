package com.janboerman.f2pstarassist.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.util.PathConverter;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.logging.Logger;

public class Main {

    public static void main(String[] args) throws Exception {

        final OptionParser optionParser = new OptionParser();
        final OptionSpec<Path> configSpec = optionParser.accepts("config-file", "json file with configurations")
                .withRequiredArg()
                .withValuesConvertedBy(new PathConverter())
                .defaultsTo(Path.of("starserver-config.json"));
        final OptionSpec<Integer> portSpec = optionParser.accepts("port", "port number on which to run the web server")
                .availableUnless(configSpec)
                .withRequiredArg()
                .withValuesConvertedBy(new PortConverter())
                .defaultsTo(80);
        optionParser.accepts("ssl", "use HTTPS instead of HTTP").availableUnless(configSpec);
        final OptionSpec<Path> keyStorePathSpec = optionParser.accepts("key-store-path", "path to the key store to use for SSL")
                .requiredIf("ssl")
                .withRequiredArg()
                .withValuesConvertedBy(new PathConverter());
        final OptionSpec<String> keyStorePasswordSpec = optionParser.accepts("key-store-password", "password to use for the key store")
                .requiredIf("ssl")
                .withRequiredArg();

        final OptionSet optionSet = optionParser.parse(args);

        final Path configFile = optionSet.valueOf(configSpec);
        final Gson gson = new GsonBuilder().setPrettyPrinting().create();

        Configuration config;
        if (Files.notExists(configFile)) {
            config = new Configuration();
            Files.writeString(configFile, gson.toJson(config.toJson()), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        } else {
            String configFileContents = Files.readString(configFile, StandardCharsets.UTF_8);
            config = Configuration.fromJson(gson.fromJson(configFileContents, JsonObject.class));
        }

        final int port = optionSet.has(portSpec) ? optionSet.valueOf(portSpec) : config.port();
        final boolean ssl = optionSet.has("ssl") || config.ssl();
        final Path keyStorePath = optionSet.has(keyStorePathSpec) ? optionSet.valueOf(keyStorePathSpec) : config.keyStore();
        final String keyStorePassword = optionSet.has(keyStorePathSpec) ? optionSet.valueOf(keyStorePasswordSpec) : config.keyStorePassword();

        config = new Configuration(port, ssl, keyStorePath, keyStorePassword);

        final StarServer starServer = new StarServer(config, Logger.getLogger("star server"));
        starServer.start();
    }

}
