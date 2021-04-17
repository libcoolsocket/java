package org.monora.coolsocket.core.variant.factory;

import org.monora.coolsocket.core.config.DefaultConfigFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class TestConfigFactory extends DefaultConfigFactory
{
    public static final int SOCKET_PORT = 56323;

    public static final SocketAddress SOCKET_ADDRESS = new InetSocketAddress(SOCKET_PORT);

    public static final int TIMEOUT_ACCEPT = 2000;

    public static final int TIMEOUT_READ = 2000;

    public TestConfigFactory()
    {
        super(SOCKET_ADDRESS, TIMEOUT_ACCEPT, TIMEOUT_READ);
    }
}
