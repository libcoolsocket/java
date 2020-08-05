package org.monora.coolsocket.core.server;

public class DefaultServerExecutorFactory implements ServerExecutorFactory
{
    @Override
    public ServerExecutor createServerExecutor()
    {
        return new DefaultServerExecutor();
    }
}
