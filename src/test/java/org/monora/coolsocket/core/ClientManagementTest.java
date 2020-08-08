package org.monora.coolsocket.core;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.monora.coolsocket.core.client.ClientHandler;
import org.monora.coolsocket.core.server.ConnectionManager;
import org.monora.coolsocket.core.server.ConnectionManagerFactory;
import org.monora.coolsocket.core.server.DefaultConnectionManager;
import org.monora.coolsocket.core.session.ActiveConnection;
import org.monora.coolsocket.core.session.CancelledException;
import org.monora.coolsocket.core.session.ClosedException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;

public class ClientManagementTest
{
    public static final int PORT = 58433;

    public static final String MSG = "HEY!";

    private final CoolSocket coolSocket = new CoolSocket(PORT);

    void startDelayedShutdown(CoolSocket coolSocket)
    {
        new Thread(new CloseRunnable(coolSocket)).start();
    }

    @Before
    public void setUp()
    {
        coolSocket.setClientHandler(new LoopClientHandler());
    }

    @After
    public void tearApart()
    {
        if (coolSocket.isListening()) {
            try {
                coolSocket.stop();
            } catch (InterruptedException ignored) {
            }
        }
    }

    @Test(expected = ClosedException.class)
    public void closingSafelyContractTest() throws IOException, InterruptedException
    {
        coolSocket.setConnectionManagerFactory(new CustomConnectionManagerFactory(true,
                ConnectionManager.CLOSING_CONTRACT_CLOSE_SAFELY));
        coolSocket.start();

        startDelayedShutdown(coolSocket);

        try (ActiveConnection activeConnection = ActiveConnection.connect(new InetSocketAddress(PORT))) {
            while (activeConnection.getSocket().isConnected())
                activeConnection.receive();
        }
    }

    @Test(expected = CancelledException.class)
    public void cancelContractTest() throws IOException, InterruptedException
    {
        coolSocket.setConnectionManagerFactory(new CustomConnectionManagerFactory(true,
                ConnectionManager.CLOSING_CONTRACT_CANCEL));
        coolSocket.start();

        startDelayedShutdown(coolSocket);

        try (ActiveConnection activeConnection = ActiveConnection.connect(new InetSocketAddress(PORT))) {
            while (activeConnection.getSocket().isConnected())
                activeConnection.receive();
        }
    }

    @Test(expected = SocketException.class)
    public void closeImmediatelyContractTest() throws IOException, InterruptedException
    {
        coolSocket.setConnectionManagerFactory(new CustomConnectionManagerFactory(true,
                ConnectionManager.CLOSING_CONTRACT_CLOSE_IMMEDIATELY));
        coolSocket.start();

        startDelayedShutdown(coolSocket);

        try (ActiveConnection activeConnection = ActiveConnection.connect(new InetSocketAddress(PORT))) {
            while (activeConnection.getSocket().isConnected())
                activeConnection.receive();
        }
    }

    public static class LoopClientHandler implements ClientHandler
    {
        @Override
        public void onConnected(ActiveConnection activeConnection)
        {
            try {
                while (activeConnection.getSocket().isConnected())
                    activeConnection.reply(MSG);
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

    public static class CloseRunnable implements Runnable
    {
        private final CoolSocket coolSocket;

        public CloseRunnable(CoolSocket coolSocket)
        {
            this.coolSocket = coolSocket;
        }

        @Override
        public void run()
        {
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