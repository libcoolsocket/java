package org.monora.coolsocket.core.variant;

import org.monora.coolsocket.core.CoolSocket;
import org.monora.coolsocket.core.config.ConfigFactory;
import org.monora.coolsocket.core.session.ActiveConnection;

import java.io.IOException;
import java.net.SocketAddress;

/**
 * This will send a static message that you can easily set via {@link #setStaticMessage(String)}.
 */
public class StaticMessageCoolSocket extends CoolSocket
{
    private String message = null;

    public StaticMessageCoolSocket(int port)
    {
        super(port);
    }

    public StaticMessageCoolSocket(SocketAddress address)
    {
        super(address);
    }

    public StaticMessageCoolSocket(ConfigFactory configFactory)
    {
        super(configFactory);
    }

    @Override
    public void onConnected(ActiveConnection activeConnection)
    {
        if (message == null)
            throw new IllegalStateException("The message should not be null");

        try {
            activeConnection.reply(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Set the message to be delivered to the clients.
     *
     * @param message To be delivered.
     */
    public void setStaticMessage(String message)
    {
        this.message = message;
    }
}
