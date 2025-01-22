package org.monora.coolsocket.core.server;

import org.jetbrains.annotations.NotNull;

/**
 * The default connection manager factory implementation.
 */
public class DefaultConnectionManagerFactory implements ConnectionManagerFactory {
    /**
     * Create a new connection manager.
     *
     * @return The newly created connection manager.
     */
    @Override
    public @NotNull ConnectionManager createConnectionManager() {
        return new DefaultConnectionManager();
    }
}
