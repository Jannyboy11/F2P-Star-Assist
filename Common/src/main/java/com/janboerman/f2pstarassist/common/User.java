package com.janboerman.f2pstarassist.common;

public interface User /*permits RunescapeUser, DiscordUser, UnknownUser*/ {

    public static User unknown() {
        return Unknown.INSTANCE;
    }

    public static class Unknown implements User {

        private static final Unknown INSTANCE = new Unknown();

        private Unknown() {
        }

        @Override
        public String toString() {
            return "Anonymous user";
        }
    }

}
