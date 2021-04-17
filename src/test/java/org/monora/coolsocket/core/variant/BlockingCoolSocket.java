package org.monora.coolsocket.core.variant;

import org.jetbrains.annotations.NotNull;
import org.monora.coolsocket.core.CoolSocket;
import org.monora.coolsocket.core.response.Response;
import org.monora.coolsocket.core.session.ActiveConnection;
import org.monora.coolsocket.core.variant.factory.TestConfigFactory;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;

/**
 * Blocking CoolSocket waits until it receives a response and makes it available for outside use via
 * {@link #waitForResponse()}.
 */
public class BlockingCoolSocket extends CoolSocket
{
    private final BlockingQueue<Response> responseQueue = new SynchronousQueue<>();

    public BlockingCoolSocket()
    {
        super(new TestConfigFactory());
    }

    @Override
    public final void onConnected(@NotNull ActiveConnection activeConnection)
    {
        try {
            responseQueue.put(activeConnection.receive());
        } catch (@NotNull IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Wait for the client response to become available and return when there is one ready.
     *
     * @return The response the client sent.
     * @throws InterruptedException If the calling thread goes into the interrupted state.
     */
    public @NotNull Response waitForResponse() throws InterruptedException
    {
        return responseQueue.take();
    }
}
