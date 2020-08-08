package org.monora.coolsocket.core;

import org.junit.Test;
import org.monora.coolsocket.core.session.ActiveConnection;
import org.monora.coolsocket.core.session.DescriptionMismatchException;

import java.io.IOException;
import java.net.InetSocketAddress;

public class SafetyTest
{
    public static final int PORT = 43244;

    @Test(expected = DescriptionMismatchException.class)
    public void blockWritingToForeignDescription() throws IOException, InterruptedException
    {
        final byte[] bytes = new byte[10];

        CoolSocket coolSocket = new CoolSocket(PORT)
        {
            @Override
            public void onConnected(ActiveConnection activeConnection)
            {
                try {
                    ActiveConnection.Description description1 = activeConnection.readBegin();
                    ActiveConnection.Description description2 = activeConnection.readBegin();

                    // reads the 2nd
                    activeConnection.read(description2);
                } catch (DescriptionMismatchException ignored) {
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (ActiveConnection activeConnection = ActiveConnection.connect(new InetSocketAddress(PORT))) {
            ActiveConnection.Description description1 = activeConnection.writeBegin(0);
            ActiveConnection.Description description2 = activeConnection.writeBegin(0);

            // writes to 1st
            activeConnection.write(description1, bytes);
        } finally {
            coolSocket.stop();
        }
    }

    @Test(expected = DescriptionMismatchException.class)
    public void blockReadingFromForeignDescription() throws IOException, InterruptedException
    {
        final byte[] bytes = new byte[10];

        CoolSocket coolSocket = new CoolSocket(PORT)
        {
            @Override
            public void onConnected(ActiveConnection activeConnection)
            {
                try {
                    ActiveConnection.Description description1 = activeConnection.writeBegin(0);
                    ActiveConnection.Description description2 = activeConnection.writeBegin(0);

                    // writes to 1st
                    activeConnection.write(description1, bytes);
                } catch (DescriptionMismatchException ignored) {
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (ActiveConnection activeConnection = ActiveConnection.connect(new InetSocketAddress(PORT))) {
            ActiveConnection.Description description1 = activeConnection.readBegin();
            ActiveConnection.Description description2 = activeConnection.readBegin();

            // reads the 2nd
            activeConnection.read(description2);
        } finally {
            coolSocket.stop();
        }
    }
}
