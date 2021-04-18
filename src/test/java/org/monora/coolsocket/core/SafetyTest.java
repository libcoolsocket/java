package org.monora.coolsocket.core;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.monora.coolsocket.core.session.ActiveConnection;
import org.monora.coolsocket.core.session.DescriptionMismatchException;
import org.monora.coolsocket.core.variant.Connections;
import org.monora.coolsocket.core.variant.DefaultCoolSocket;

import java.io.IOException;

public class SafetyTest
{
    @Test(expected = DescriptionMismatchException.class)
    public void blockReadingFromForeignDescription() throws IOException, InterruptedException
    {
        final byte[] bytes = new byte[10];

        CoolSocket coolSocket = new DefaultCoolSocket()
        {
            @Override
            public void onConnected(@NotNull ActiveConnection activeConnection)
            {
                try {
                    ActiveConnection.Description description1 = activeConnection.writeBegin(0);
                    activeConnection.writeBegin(0);

                    // writes to 1st
                    activeConnection.write(description1, bytes);
                } catch (IOException ignored) {
                }
            }
        };

        coolSocket.start();

        try (ActiveConnection activeConnection = Connections.connect()) {
            activeConnection.readBegin();
            ActiveConnection.Description description2 = activeConnection.readBegin();

            // reads the 2nd
            activeConnection.read(description2);
        } finally {
            coolSocket.stop();
        }
    }
}
