package com.janboerman.f2pstarassist.web;

import com.google.gson.JsonObject;

import java.nio.file.Path;

public record Configuration(int port, boolean ssl, Path keyStore, String keyStorePassword) {

    public Configuration() {
        this(false);
    }

    public Configuration(boolean ssl) {
        this(defaultPort(ssl), ssl, defaultKeyStore(), defaultPassword());
    }

    static final int defaultPort(boolean ssl) {
        return ssl ? 443 : 80;
    }

    static final Path defaultKeyStore() {
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null) {
            return Path.of(javaHome, "lib", "security", "cacerts");
        } else {
            return Path.of("f2p-star-assist.keystore");
        }
    }

    static final String defaultPassword() {
        return "changeit";
    }

    public JsonObject toJson() {
        JsonObject result = new JsonObject();
        result.addProperty("port", port());
        result.addProperty("ssl", ssl());
        result.addProperty("key store", keyStore().toString());
        result.addProperty("key store password", keyStorePassword());
        return result;
    }

    public static Configuration fromJson(JsonObject json) {
        int port = json.getAsJsonPrimitive("port").getAsInt();
        boolean ssl = json.getAsJsonPrimitive("ssl").getAsBoolean();
        Path keyStore = Path.of(json.getAsJsonPrimitive("key store").getAsString());
        String password = json.getAsJsonPrimitive("key store password").getAsString();
        return new Configuration(port, ssl, keyStore, password);
    }
}
