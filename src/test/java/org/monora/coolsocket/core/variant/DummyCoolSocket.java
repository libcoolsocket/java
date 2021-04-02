package org.monora.coolsocket.core.variant;

import org.monora.coolsocket.core.CoolSocket;
import org.monora.coolsocket.core.config.ConfigFactory;
import org.monora.coolsocket.core.session.ActiveConnection;

import java.net.SocketAddress;

/**
 * This is not meant to receive client requests.
 */
public class DummyCoolSocket extends CoolSocket
{
    public DummyCoolSocket(int port)
    {
        super(port);
    }

    public DummyCoolSocket(SocketAddress address)
    {
        super(address);
    }

    public DummyCoolSocket(ConfigFactory configFactory)
    {
        super(configFactory);
    }

    @Override
    public void onConnected(ActiveConnection activeConnection)
    {

    }
}
