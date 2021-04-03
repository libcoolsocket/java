package org.monora.coolsocket.core.server;

import org.jetbrains.annotations.NotNull;

public class DefaultConnectionManagerFactory implements ConnectionManagerFactory
{
    @Override
    public @NotNull ConnectionManager createConnectionManager()
    {
        return new DefaultConnectionManager();
    }
}
