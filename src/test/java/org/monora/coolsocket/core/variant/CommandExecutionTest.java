package org.monora.coolsocket.core.variant;

import org.monora.coolsocket.core.ActiveConnection;
import org.monora.coolsocket.core.CancelledException;
import org.monora.coolsocket.core.CoolSocket;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;

public class CommandExecutionTest
{
    private static final int PORT = 3547;

    @Test(expected = CancelledException.class)
    public void cancellationDuringWriteBeginWorksTest() throws IOException, InterruptedException
    {
        CoolSocket coolSocket = new CoolSocket(PORT)
        {
            @Override
            public void onConnected(ActiveConnection activeConnection)
            {
                activeConnection.cancel();
                try {
                    activeConnection.writeBegin(0, LENGTH_UNSPECIFIED);
                } catch (CancelledException ignored) {
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (ActiveConnection activeConnection = ActiveConnection.connect(new InetSocketAddress(PORT))) {
            activeConnection.readBegin();
        } finally {
            coolSocket.stop();
        }
    }

    @Test(expected = CancelledException.class)
    public void cancellationDuringReadBeginWorksTest() throws IOException, InterruptedException
    {
        CoolSocket coolSocket = new CoolSocket(PORT)
        {
            @Override
            public void onConnected(ActiveConnection activeConnection)
            {
                activeConnection.cancel();
                try {
                    activeConnection.readBegin();
                } catch (CancelledException ignored) {
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (ActiveConnection activeConnection = ActiveConnection.connect(new InetSocketAddress(PORT))) {
            activeConnection.writeBegin(0, CoolSocket.LENGTH_UNSPECIFIED);
        } finally {
            coolSocket.stop();
        }
    }

    @Test
    public void communicationAfterCancellationWorksTest() throws IOException, InterruptedException
    {
        final String message = "Where we are from there is no sun.";

        CoolSocket coolSocket = new CoolSocket(PORT)
        {
            @Override
            public void onConnected(ActiveConnection activeConnection)
            {
                activeConnection.cancel();
                try {
                    activeConnection.readBegin();
                } catch (CancelledException ignored) {
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    activeConnection.reply(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        ActiveConnection activeConnection = ActiveConnection.connect(new InetSocketAddress(PORT));

        try {
            activeConnection.writeBegin(0, CoolSocket.LENGTH_UNSPECIFIED);
        } catch (CancelledException ignored) {
        } catch (IOException e) {
            e.printStackTrace();
        }

        Assert.assertEquals("The messages should be the same.", message,
                activeConnection.receive().getAsString());

        activeConnection.close();
        coolSocket.stop();
    }

    @Test
    public void exchangeProtocolVersionTest() throws IOException, InterruptedException
    {
        final String message = "It is a long way home but a fun one.";
        StaticMessageCoolSocket coolSocket = new StaticMessageCoolSocket(PORT);
        coolSocket.setStaticMessage(message);
        coolSocket.start();

        try (ActiveConnection activeConnection = ActiveConnection.connect(new InetSocketAddress(PORT))) {
            activeConnection.receive();

            Assert.assertEquals("The protocol version should be the same.",
                    ActiveConnection.getProtocolVersion(), activeConnection.getProtocolVersionOfRemote());
        } finally {
            coolSocket.stop();
        }
    }
}
