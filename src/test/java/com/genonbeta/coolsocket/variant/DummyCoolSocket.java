package com.genonbeta.coolsocket.variant;

import com.genonbeta.coolsocket.ActiveConnection;
import com.genonbeta.coolsocket.ConfigFactory;
import com.genonbeta.coolsocket.CoolSocket;

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
