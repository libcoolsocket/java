package org.monora.coolsocket.core;

import org.junit.Ignore;
import org.junit.Test;
import org.monora.coolsocket.core.client.ClientHandler;
import org.monora.coolsocket.core.server.ConnectionManager;
import org.monora.coolsocket.core.server.ConnectionManagerFactory;
import org.monora.coolsocket.core.server.DefaultConnectionManager;
import org.monora.coolsocket.core.session.ActiveConnection;
import org.monora.coolsocket.core.session.ClosedException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;

@Ignore
public class ClientManagementTest
{
    public static final int PORT = 58433;

    @Test(expected = SocketException.class)
    public void closingServerClosesClientsTest() throws IOException, InterruptedException
    {
        final CoolSocket coolSocket = new CoolSocket(PORT);
        coolSocket.setClientHandler(new LoopClientHandler(coolSocket));
        coolSocket.start();

        try (ActiveConnection activeConnection = ActiveConnection.connect(new InetSocketAddress(PORT))) {
            while (activeConnection.getSocket().isConnected())
                activeConnection.receive();
        } finally {
            coolSocket.stop();
        }
    }

    @Test(expected = ClosedException.class)
    public void closingSafelyContractTest() throws IOException, InterruptedException
    {
        final CoolSocket coolSocket = new CoolSocket(PORT);
        coolSocket.setClientHandler(new LoopClientHandler(coolSocket));
        coolSocket.setConnectionManagerFactory(new CustomConnectionManagerFactory(true,
                ConnectionManager.CLOSING_CONTRACT_CLOSE_SAFELY));
        coolSocket.start();

        try (ActiveConnection activeConnection = ActiveConnection.connect(new InetSocketAddress(PORT))) {
            while (activeConnection.getSocket().isConnected())
                activeConnection.receive();
        } finally {
            coolSocket.stop();
        }
    }

    public static class LoopClientHandler implements ClientHandler
    {
        private final CoolSocket coolSocket;

        public LoopClientHandler(CoolSocket coolSocket)
        {
            this.coolSocket = coolSocket;
        }

        @Override
        public void onConnected(ActiveConnection activeConnection)
        {
            try {
                for (int i = 0; i < 500; i++) {
                    activeConnection.reply("Hey!");
                    if (i == 10)
                        coolSocket.stop();
                }
            } catch (Exception ignored) {
            }
        }
    }

    public static class CustomConnectionManagerFactory implements ConnectionManagerFactory
    {
        private final boolean waitForExit;

        private final int closingContract;

        public CustomConnectionManagerFactory(boolean wait, int closingContract)
        {
            this.waitForExit = wait;
            this.closingContract = closingContract;
        }

        @Override
        public ConnectionManager createConnectionManager()
        {
            DefaultConnectionManager connectionManager = new DefaultConnectionManager();
            connectionManager.setClosingContract(waitForExit, closingContract);

            return connectionManager;
        }
    }
}
