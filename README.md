# F2P StarHunt

A plugin for crowdsourcing shooting stars in F2P.

## Features

- Sidebar panel with list of known stars.
    - Double-click a star entry to hop to the world of a star.
    - Right-click to remove entries.
- Analyzes chat messages for star calls.
- Arrow hint for a star found in your world.
- Optional: Communication with a webserver to share your stars with others.

## Compiling

Prerequisites:
- [JDK17](https://jdk.java.net/17/)
- [Apache Maven](https://maven.apache.org/)

Then run `mvn clean package` to generate the jar files.