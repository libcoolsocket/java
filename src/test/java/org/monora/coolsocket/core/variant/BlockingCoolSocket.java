package org.monora.coolsocket.core.variant;

import org.monora.coolsocket.core.ActiveConnection;
import org.monora.coolsocket.core.config.ConfigFactory;
import org.monora.coolsocket.core.CoolSocket;
import org.monora.coolsocket.core.response.Response;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;

/**
 * Blocking CoolSocket waits until it receives a response and makes it available for outside use via
 * {@link #waitForResponse()}.
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
    public final void onConnected(ActiveConnection activeConnection)
    {
        try {
            responseQueue.put(activeConnection.receive());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Wait for the client response to become available and return when there is one ready.
     *
     * @return The response the client sent.
     * @throws InterruptedException If the calling thread goes into the interrupted state.
     */
    public Response waitForResponse() throws InterruptedException
    {
        return responseQueue.take();
    }
}
