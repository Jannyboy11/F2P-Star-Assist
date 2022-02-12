package com.janboerman.starhunt.common;

import java.util.Objects;

public final class GroupKey {

    private String key;

    public GroupKey(String key) {
        this.key = Objects.requireNonNull(key, "key cannot be null");
    }

    //TODO delete this.
    public static GroupKey fromPlain(String rawKey) {
        Objects.requireNonNull(rawKey, "rawKey cannot be null");

        return new GroupKey(rawKey);
    }

    @Deprecated //TODO delete this
    public static GroupKey fromEncoded(String encodedKey) {
        return new GroupKey(encodedKey);
    }

    @Deprecated //TODO delete this.
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
        return key;
    }
}
