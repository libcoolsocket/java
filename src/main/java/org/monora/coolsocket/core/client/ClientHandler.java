package org.monora.coolsocket.core.client;

import org.jetbrains.annotations.NotNull;
import org.monora.coolsocket.core.session.Channel;

/**
 * This class handles the last sequence of a client connection which is to answer to it.
 * <p>
 * This is implemented by {@link org.monora.coolsocket.core.CoolSocket} by default can be overridden.
 */
public interface ClientHandler {
    /**
     * When a client is connected, this method will be called.
     *
     * @param channel The connection object that represents the client.
     */
    default void onConnected(@NotNull Channel channel) {

    }
}
