package com.janboerman.starhunt.common;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class GroupKey {

    private final String key;

    private GroupKey(String key) {
        this.key = Objects.requireNonNull(key, "key cannot be null");
    }

    public static GroupKey fromRaw(String rawKey) {
        Objects.requireNonNull(rawKey, "rawKey cannot be null");
        try {
            String encoded = URLEncoder.encode(rawKey, StandardCharsets.UTF_8.name());
            return new GroupKey(encoded);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("impossible", e);
        }
    }

    public static GroupKey fromEncoded(String encodedKey) {
        return new GroupKey(encodedKey);
    }

    public String encoded() {
        return key;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof GroupKey)) return false;

        GroupKey that = (GroupKey) o;
        return Objects.equals(this.key, that.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return encoded();
    }
}
