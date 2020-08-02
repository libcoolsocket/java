package com.genonbeta.coolsocket;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;

/**
 * Blocking CoolSocket waits until it receives a response and makes it available for outside use via
 * {@link #waitForResponse()}
 */
public class BlockingCoolSocket extends CoolSocket
{
    private final BlockingQueue<Response> responseQueue = new SynchronousQueue<>();

    public BlockingCoolSocket(int port)
    {
        super(port);
    }

    public BlockingCoolSocket(SocketAddress address)
    {
        super(address);
    }

    public BlockingCoolSocket(ConfigFactory configFactory)
    {
        super(configFactory);
    }

    @Override
    public void onConnected(ActiveConnection activeConnection)
    {
        try {
            responseQueue.offer(activeConnection.receive());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Response waitForResponse() throws InterruptedException
    {
        return responseQueue.take();
    }
}
