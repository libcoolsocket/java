package com.genonbeta.coolsocket.server;

public class DefaultServerExecutorFactory implements ServerExecutorFactory
{
    @Override
    public ServerExecutor createServerExecutor()
    {
        return new DefaultServerExecutor();
    }
}
