package org.monora.coolsocket.core;

import org.junit.Test;
import org.monora.coolsocket.core.session.ActiveConnection;

import java.io.IOException;
import java.net.InetSocketAddress;

public class CommunicationTest
{
    public static final int PORT = 54234;

    @Test
    public void exchangeReceiveFailsWhenSizeExceedsTest() throws IOException, InterruptedException
    {
        CoolSocket coolSocket = new CoolSocket(PORT)
        {
            @Override
            public void onConnected(ActiveConnection activeConnection)
            {

            }
        };

        coolSocket.start();

        try (ActiveConnection activeConnection = ActiveConnection.connect(new InetSocketAddress(PORT))) {
            //activeConnection.handleByteBreak();
        } finally {
            coolSocket.stop();
        }
    }
}
