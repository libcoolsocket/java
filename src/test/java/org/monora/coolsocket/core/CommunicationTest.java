package org.monora.coolsocket.core;

import org.junit.Test;
import org.monora.coolsocket.core.session.ActiveConnection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;

public class CommunicationTest
{
    public static final int PORT = 54234;

    @Test
    public void speedTest() throws IOException, InterruptedException
    {
        final int repeat = 100000;
        final byte[] data = new byte[8096];

        Arrays.fill(data, (byte) 2);

        final CoolSocket coolSocket = new CoolSocket(PORT)
        {
            @Override
            public void onConnected(ActiveConnection activeConnection)
            {
                try {
                    ActiveConnection.Description description = activeConnection.writeBegin(0,
                            data.length * repeat);
                    while (description.hasAvailable()) {
                        activeConnection.write(description, data);
                    }
                    activeConnection.writeEnd(description);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (ActiveConnection activeConnection = ActiveConnection.connect(new InetSocketAddress(PORT))) {
            long startTime = System.nanoTime();
            ActiveConnection.Description description = activeConnection.readBegin();
            do {
                int len = activeConnection.read(description);
                if (len > 0) {

                }
            } while (description.hasAvailable());
            System.out.println("It took: " + ((System.nanoTime() - startTime) / 1e9));
        } finally {
            coolSocket.stop();
        }
    }
}
