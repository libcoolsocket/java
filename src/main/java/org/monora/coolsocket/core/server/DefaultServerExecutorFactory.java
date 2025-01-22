package org.monora.coolsocket.core.server;

import org.jetbrains.annotations.NotNull;

/**
 * The default server executor factory implementation.
 */
public class DefaultServerExecutorFactory implements ServerExecutorFactory {
    @Override
    public @NotNull ServerExecutor createServerExecutor() {
        return new DefaultServerExecutor();
    }
}
