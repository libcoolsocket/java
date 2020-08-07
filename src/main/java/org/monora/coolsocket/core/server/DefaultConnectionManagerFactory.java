package org.monora.coolsocket.core.server;

public class DefaultConnectionManagerFactory implements ConnectionManagerFactory
{
    @Override
    public ConnectionManager createConnectionManager()
    {
        return new DefaultConnectionManager();
    }
}
