package org.monora.coolsocket.core;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.monora.coolsocket.core.session.ActiveConnection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ClientManagementTest
{
    public static final int PORT = 58433;

    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(6, 10, 0, TimeUnit.MILLISECONDS,
            new SynchronousQueue<>());
    private final CoolSocket coolSocket = new VeryCoolSocket();

    @Before
    public void setUp() throws IOException, InterruptedException
    {
        coolSocket.start();
    }

    @After
    public void tearApart() throws InterruptedException
    {
        if (coolSocket.isListening())
            coolSocket.stop();
    }

    @Test
    public void closingServerClosesClientsTest() throws IOException, InterruptedException
    {
        ClientRunnable[] clients = new ClientRunnable[6];

        for (int i = 0; i < clients.length; i++) {
            clients[i] = new ClientRunnable();
        }

        for (ClientRunnable runnable : clients) {
            executor.execute(runnable);
        }

        coolSocket.stop();

        executor.awaitTermination(0, TimeUnit.MILLISECONDS);


        for (ClientRunnable runnable : clients) {
            Assert.assertTrue("The closed exception should be thrown.",
                    runnable.exception instanceof IOException);
        }
    }

    private static class VeryCoolSocket extends CoolSocket
    {
        public VeryCoolSocket()
        {
            super(PORT);
        }

        @Override
        public void onConnected(ActiveConnection activeConnection)
        {
            try {
                while (activeConnection.getSocket().isConnected())
                    activeConnection.receive();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static class ClientRunnable implements Runnable
    {
        public Exception exception;

        @Override
        public void run()
        {
            try (ActiveConnection activeConnection = ActiveConnection.connect(new InetSocketAddress(PORT))) {
                while (activeConnection.getSocket().isConnected())
                    activeConnection.reply("Hey!");
            } catch (Exception e) {
                exception = e;
            }
        }
    }
}
