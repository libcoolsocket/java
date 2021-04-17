package org.monora.coolsocket.core;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.monora.coolsocket.core.config.Config;
import org.monora.coolsocket.core.session.ActiveConnection;
import org.monora.coolsocket.core.session.CancelledException;
import org.monora.coolsocket.core.session.ClosedException;
import org.monora.coolsocket.core.variant.Connections;
import org.monora.coolsocket.core.variant.DefaultCoolSocket;
import org.monora.coolsocket.core.variant.StaticMessageCoolSocket;
import org.monora.coolsocket.core.variant.factory.TestConfigFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;

public class CommandExecutionTest
{
    @Test(expected = CancelledException.class)
    public void cancellationDuringWriteBeginTest() throws IOException, InterruptedException
    {
        CoolSocket coolSocket = new DefaultCoolSocket()
        {
            @Override
            public void onConnected(@NotNull ActiveConnection activeConnection)
            {
                try {
                    activeConnection.cancel();
                    activeConnection.writeBegin(0);
                } catch (CancelledException ignored) {
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (ActiveConnection activeConnection = Connections.connect()) {
            activeConnection.readBegin();
        } finally {
            coolSocket.stop();
        }
    }

    @Test(expected = CancelledException.class)
    public void cancellationDuringReadBeginTest() throws IOException, InterruptedException
    {
        CoolSocket coolSocket = new DefaultCoolSocket() 
        {
            @Override
            public void onConnected(@NotNull ActiveConnection activeConnection)
            {
                try {
                    activeConnection.cancel();
                    activeConnection.readBegin();
                } catch (CancelledException ignored) {
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (ActiveConnection activeConnection = Connections.connect()) {
            activeConnection.writeBegin(0);
        } finally {
            coolSocket.stop();
        }
    }

    @Test
    public void communicationAfterCancellationTest() throws IOException, InterruptedException
    {
        final String message = "Where we are from there is no sun.";

        CoolSocket coolSocket = new DefaultCoolSocket()
        {
            @Override
            public void onConnected(@NotNull ActiveConnection activeConnection)
            {
                try {
                    activeConnection.cancel();
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

        ActiveConnection activeConnection = Connections.connect();

        try {
            activeConnection.writeBegin(0);
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
    public void cancellationDuringReadTest() throws IOException, InterruptedException
    {
        final String message = "The stars and moon are there! And we are going to climb it!";

        CoolSocket coolSocket = new DefaultCoolSocket()
        {
            @Override
            public void onConnected(@NotNull ActiveConnection activeConnection)
            {
                try {
                    ActiveConnection.Description description = activeConnection.readBegin();
                    activeConnection.cancel();
                    try {
                        activeConnection.read(description);
                    } catch (CancelledException ignored) {
                        activeConnection.reply(message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (ActiveConnection activeConnection = Connections.connect()) {
            ActiveConnection.Description description = activeConnection.writeBegin(0);

            try {
                activeConnection.write(description, message.getBytes());
            } catch (CancelledException ignored) {
                Assert.assertEquals("The messages should match.", message,
                        activeConnection.receive().getAsString());
            }
        } finally {
            coolSocket.stop();
        }
    }

    @Test
    public void exchangeProtocolVersionTest() throws IOException, InterruptedException
    {
        final String message = "It is a long way home but a fun one.";
        StaticMessageCoolSocket coolSocket = new StaticMessageCoolSocket();
        coolSocket.setStaticMessage(message);
        coolSocket.start();

        try (ActiveConnection activeConnection = Connections.connect()) {
            activeConnection.receive();

            Assert.assertEquals("The protocol version should be the same.",
                    ActiveConnection.getProtocolVersion(), activeConnection.getProtocolVersionOfRemote());
            Assert.assertEquals("The protocol version should be the same.",
                    Config.PROTOCOL_VERSION, activeConnection.getProtocolVersionOfRemote());
        } finally {
            coolSocket.stop();
        }
    }

    @Test(expected = ClosedException.class)
    public void closeSafelyTest() throws IOException, InterruptedException
    {
        final String message = "It is a long way home but a fun one.";
        CoolSocket coolSocket = new DefaultCoolSocket()
        {
            @Override
            public void onConnected(@NotNull ActiveConnection activeConnection)
            {
                try {
                    activeConnection.closeSafely();
                    activeConnection.reply(message);
                } catch (ClosedException ignored) {
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (ActiveConnection activeConnection = Connections.connect()) {
            activeConnection.receive();
        } finally {
            coolSocket.stop();
        }
    }

    @Test
    public void closeSafelyRemoteCloseProcessedTest() throws IOException, InterruptedException
    {
        final String message = "It is a long way home but a fun one.";
        CoolSocket coolSocket = new DefaultCoolSocket()
        {
            @Override
            public void onConnected(@NotNull ActiveConnection activeConnection)
            {
                try {
                    activeConnection.closeSafely();
                    activeConnection.reply(message);
                } catch (ClosedException ignored) {
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (ActiveConnection activeConnection = Connections.connect()) {
            activeConnection.receive();
        } catch (ClosedException e) {
            Assert.assertTrue("The close operation should be requested by the remote.", e.remoteRequested);
        } finally {
            coolSocket.stop();
        }
    }

    @Test(expected = SocketException.class)
    public void closeSafelyClosesConnectionTest() throws IOException, InterruptedException
    {
        final String message = "It is a long way home but a fun one.";
        CoolSocket coolSocket = new DefaultCoolSocket()
        {
            @Override
            public void onConnected(@NotNull ActiveConnection activeConnection)
            {
                try {
                    activeConnection.closeSafely();
                    activeConnection.reply(message);
                } catch (ClosedException ignored) {
                    try {
                        activeConnection.receive();
                    } catch (SocketException e) {
                        // This is what is expected.
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (ActiveConnection activeConnection = Connections.connect()) {
            try {
                activeConnection.receive();
            } catch (ClosedException e) {
                activeConnection.reply(message);
            }
        } finally {
            coolSocket.stop();
        }
    }

    @Test(expected = CancelledException.class)
    public void readerCancelsWhenReadingLargeChunksTest() throws InterruptedException, IOException
    {
        final byte[] data = new byte[8192];
        final CoolSocket coolSocket = new DefaultCoolSocket()
        {
            @Override
            public void onConnected(@NotNull ActiveConnection activeConnection)
            {
                try {
                    ActiveConnection.Description description = activeConnection.readBegin();
                    while (description.hasAvailable()) {
                        activeConnection.cancel();
                        activeConnection.read(description);
                    }

                } catch (CancelledException ignored) {
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        try (ActiveConnection activeConnection = Connections.connect()) {
            ActiveConnection.Description description = activeConnection.writeBegin(0);
            while (activeConnection.getSocket().isConnected()) {
                activeConnection.write(description, data);
            }

            activeConnection.writeEnd(description);
        } finally {
            coolSocket.stop();
        }
    }
}
