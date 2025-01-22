package org.monora.coolsocket.core.server;

import org.jetbrains.annotations.NotNull;
import org.monora.coolsocket.core.CoolSocket;

/**
 * This class produces the {@link ServerExecutor} that is consumed by {@link CoolSocket} during a session.
 * <p>
 * The produced server executor will only be valid during one session, and after that session exits, it won't be needed
 * by CoolSocket any longer. Instead, this factory will be invoked again to produce a new server executor when needed.
 */
public interface ServerExecutorFactory {
    /**
     * This method will return an {@link ServerExecutor} implementation that will handle a session.
     *
     * @return The produced server executor.
     */
    @NotNull ServerExecutor createServerExecutor();
}
