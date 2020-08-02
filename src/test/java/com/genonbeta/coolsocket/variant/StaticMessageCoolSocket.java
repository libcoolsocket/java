package com.genonbeta.coolsocket.variant;

import com.genonbeta.coolsocket.ActiveConnection;
import com.genonbeta.coolsocket.ConfigFactory;
import com.genonbeta.coolsocket.CoolSocket;

import java.net.SocketAddress;

public class StaticMessageCoolSocket extends CoolSocket
{
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

    }
}
