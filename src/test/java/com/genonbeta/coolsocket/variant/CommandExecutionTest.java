package com.genonbeta.coolsocket.variant;

import com.genonbeta.coolsocket.ActiveConnection;
import com.genonbeta.coolsocket.CancelledException;
import com.genonbeta.coolsocket.CoolSocket;
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

        try (ActiveConnection activeConnection = ActiveConnection.connect(new InetSocketAddress(PORT), 0)) {
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

        try (ActiveConnection activeConnection = ActiveConnection.connect(new InetSocketAddress(PORT), 0)) {
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

        ActiveConnection activeConnection = ActiveConnection.connect(new InetSocketAddress(PORT), 0);

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

        try (ActiveConnection activeConnection = ActiveConnection.connect(new InetSocketAddress(PORT), 0)) {
            activeConnection.receive();

            Assert.assertEquals("The protocol version should be the same.",
                    ActiveConnection.getProtocolVersion(), activeConnection.getProtocolVersionOfRemote());
        } finally {
            coolSocket.stop();
        }
    }
}
