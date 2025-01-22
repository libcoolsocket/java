package org.monora.coolsocket.core;

import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.monora.coolsocket.core.client.ClientHandler;
import org.monora.coolsocket.core.server.ConnectionManager;
import org.monora.coolsocket.core.server.ConnectionManagerFactory;
import org.monora.coolsocket.core.server.DefaultConnectionManager;
import org.monora.coolsocket.core.session.Channel;
import org.monora.coolsocket.core.session.CancelledException;
import org.monora.coolsocket.core.session.ClosedException;
import org.monora.coolsocket.core.variant.Connections;
import org.monora.coolsocket.core.variant.DefaultCoolSocket;
import org.monora.coolsocket.core.variant.factory.TestConfigFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;

public class ClientManagementTest {
    public static final String MSG = "HEY!";

    private final CoolSocket coolSocket = new DefaultCoolSocket();

    void startDelayedShutdown(CoolSocket coolSocket) {
        new Thread(new CloseRunnable(coolSocket)).start();
    }

    @Before
    public void setUp() {
        coolSocket.setClientHandler(new LoopClientHandler());
    }

    @After
    public void tearApart() {
        if (coolSocket.isListening()) {
            try {
                coolSocket.stop();
            } catch (InterruptedException ignored) {
            }
        }
    }

    @Test(expected = ClosedException.class)
    public void closingSafelyContractTest() throws IOException, InterruptedException {
        coolSocket.setConnectionManagerFactory(new CustomConnectionManagerFactory(true,
                ConnectionManager.CLOSING_CONTRACT_CLOSE_SAFELY));
        coolSocket.start();

        startDelayedShutdown(coolSocket);

        try (Channel channel = Connections.open()) {
            while (channel.getSocket().isConnected())
                channel.readAll();
        }
    }

    @Test(expected = CancelledException.class)
    public void cancelContractTest() throws IOException, InterruptedException {
        coolSocket.setConnectionManagerFactory(new CustomConnectionManagerFactory(true,
                ConnectionManager.CLOSING_CONTRACT_CANCEL));
        coolSocket.start();

        startDelayedShutdown(coolSocket);

        try (Channel channel = Connections.open()) {
            while (channel.getSocket().isConnected())
                channel.readAll();
        }
    }

    @Test(expected = SocketException.class)
    public void closeImmediatelyContractTest() throws IOException, InterruptedException {
        coolSocket.setConnectionManagerFactory(new CustomConnectionManagerFactory(true,
                ConnectionManager.CLOSING_CONTRACT_CLOSE_IMMEDIATELY));
        coolSocket.start();

        startDelayedShutdown(coolSocket);

        try (Channel channel = Connections.open()) {
            while (channel.getSocket().isConnected())
                channel.readAll();
        }
    }

    @Test
    public void countClientConnectionsTest() throws IOException, InterruptedException {
        final String message = "Hey!";

        CoolSocket coolSocket = new DefaultCoolSocket() {
            @Override
            public void onConnected(@NotNull Channel activeConnection) {
                try {
                    while (activeConnection.getSocket().isConnected())
                        activeConnection.readAll();
                } catch (ClosedException ignored) {
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        coolSocket.start();

        CoolSocket.Session session = coolSocket.getSession();
        Channel[] connections = new Channel[5];
        InetAddress localhost = InetAddress.getByName(TestConfigFactory.SOCKET_ADDRESS_HOST);

        try {
            Assert.assertNotNull(session);

            for (int i = 0; i < connections.length; i++) {
                Channel channel = Connections.open();
                connections[i] = channel;

                channel.writeAll(message.getBytes());
            }

            Assert.assertEquals("Number of connections should be same.", connections.length,
                    session.getConnectionManager().getActiveConnectionList().size());

            Assert.assertEquals("Number of connections should be same.", connections.length,
                    session.getConnectionManager().getConnectionCountByAddress(localhost));

            for (Channel channel : connections) {
                try {
                    channel.closeMutually();
                    channel.writeAll(message.getBytes());
                } catch (ClosedException ignored) {
                }
            }
        } finally {
            coolSocket.stop();
        }

        Assert.assertEquals("Connections should zeroed", 0,
                session.getConnectionManager().getActiveConnectionList().size());
    }

    public static class LoopClientHandler implements ClientHandler {
        @Override
        public void onConnected(@NotNull Channel channel) {
            try {
                while (channel.getSocket().isConnected())
                    channel.writeAll(MSG.getBytes());
            } catch (Exception ignored) {
            }
        }
    }

    public static class CustomConnectionManagerFactory implements ConnectionManagerFactory {
        private final boolean waitForExit;

        private final int closingContract;

        public CustomConnectionManagerFactory(boolean wait, int closingContract) {
            this.waitForExit = wait;
            this.closingContract = closingContract;
        }

        @Override
        public @NotNull ConnectionManager createConnectionManager() {
            DefaultConnectionManager connectionManager = new DefaultConnectionManager();
            connectionManager.setClosingContract(waitForExit, closingContract);

            return connectionManager;
        }
    }

    public static class CloseRunnable implements Runnable {
        private final CoolSocket coolSocket;

        public CloseRunnable(CoolSocket coolSocket) {
            this.coolSocket = coolSocket;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            try {
                coolSocket.stop();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
