# CoolSocket - Bidirectional TCP Socket Layer for JavaSE

CoolSocket is a scalable approach to TCP socket communication. It can send just text, or it can send large bytes just 
like usual streams. The difference is it injects its own communication bits into the bytes delivered so that your 
requests to stop an operation can be processed.

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
            public void onConnected(ActiveConnection activeConnection)
            {
                try {
                    activeConnection.reply("Hello!");
                    System.out.println(activeConnection.receive().getAsString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        ActiveConnection activeConnection = ActiveConnection.connect(new InetSocketAddress(port), 0);
        System.out.println(activeConnection.receive().getAsString());
        activeConnection.reply("Merhaba!");

        activeConnection.close();
        coolSocket.stop();
    }
}
```

## Implementing CoolSocket

### Gradle

```groovy
// ...
repositories {
    // ...
    jcenter()
}
// ...
dependencies {
    // ...
    implementation 'com.genonbeta.coolsocket:main:1.0.2'
}
// ...
```