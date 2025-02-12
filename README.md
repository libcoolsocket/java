![Maven Package](https://github.com/libcoolsocket/java/workflows/Maven%20Package/badge.svg)

# CoolSocket - Bidirectional TCP Socket Layer for JavaSE

CoolSocket is a scalable approach to TCP socket communication. It can send just text, or it can send large bytes just 
like usual streams. The difference is it injects its own communication bits into the bytes delivered so that your 
requests (e.g, stop or cancel an operation) can be processed.

Here is a quick example:

```java
import CoolSocket;
import ActiveConnection;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Main
{
    public static void main(String[] args) throws IOException, InterruptedException
    {
        final int port = 4534;
        CoolSocket coolSocket = new CoolSocket(port)
        {
            @Override
            public void onConnected(ActiveConnection channel)
            {
                try {
                    channel.reply("Hello!");
                    System.out.println(channel.receive().getAsString()); // Merhaba!
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (ActiveConnection channel = ActiveConnection.connect(new InetSocketAddress(port), 0)) {
            System.out.println(channel.receive().getAsString()); // "Hello!"
            channel.reply("Merhaba!");
        } finally {
            coolSocket.stop();   
        }
    }
}
```

## Implementing CoolSocket

See the packages for the latest version. Do not use JCenter. It will only have older versions.