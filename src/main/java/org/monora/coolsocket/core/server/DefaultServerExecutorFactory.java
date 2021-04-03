package org.monora.coolsocket.core.server;

import org.jetbrains.annotations.NotNull;

public class DefaultServerExecutorFactory implements ServerExecutorFactory
{
    @Override
    public @NotNull ServerExecutor createServerExecutor()
    {
        return new DefaultServerExecutor();
    }
}
