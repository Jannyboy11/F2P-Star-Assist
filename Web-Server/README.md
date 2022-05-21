# How 2 run this

Just run it with `java -jar F2P-Star-Assist-Web-Server.jar`!

### Commandline options

| Option name          | Possible values   | Default value            | Example                               | Description                                            |
|----------------------|-------------------|--------------------------|---------------------------------------|--------------------------------------------------------|
| --config-file        | any file path     | starserver-config.json   | --config-file starserver-config.json  | Load the startup arguments from a config file instead. |
| --port               | [0-65535]         | 80                       | --port 8080                           | Sets the port on which connections are accepted.       |
| --ssl                | {false, true}     | false                    | --ssl                                 | Enable (or disable) SSL/TLS.                           |
| --key-store-path     | any file path     | f2p-star-assist.keystore | --key-store-path /home/user/.keystore | The keystore file for the SSL certificate.             |
| --key-store-password | any string        | changeit                 | --key-store-password P@$$W0RD         | The password used for the keystore file                |

Any option that is not included on the commandline will be loaded from the server's config json file.
It is recommended that you make this file readable/writable by only the system user that runs the webserver, e.g. by using `chmod 640 starserver-config.json`.

### Using SSL

This server uses Jetty Embedded, which is configured to use a java .keystore file.
You can generate one using the JDK's built-in `keytool` commandline utility.
For example, to generate a .keystore for your Let's Encrypt SSL certificate:

```shell
#!/bin/sh

set -e
set -x

openssl pkcs12 -export \
        -in /etc/letsencrypt/live/my.domain/fullchain.pem \
        -inkey /etc/letsencrypt/live/my.domain/privkey.pem \
        -out ./f2p-star-assist.my.domain.p12 \
        -name f2p-star-assist.my.domain \
        -CAfile /etc/letsencrypt/live/my.domain/chain.pem \
        -caname "Let's Encrypt Authority X3" \
        -password pass:changeit

keytool -importkeystore \
        -deststorepass changeit \
        -destkeypass changeit \
        -deststoretype pkcs12 \
        -srckeystore ./f2p-star-assist.my.domain.p12 \
        -srcstoretype PKCS12 \
        -srcstorepass changeit \
        -destkeystore ./f2p-star-assist.my.domain.keystore

chown f2p-star-assist ./f2p-star-assist.my.domain.keystore
chmod 640 ./f2p-star-assist.my.domain.keystore
```

Feel free to delete the f2p-star-assist.my.domain.p12 file afterwards - it was only used as an intermediate step.
