package org.monora.coolsocket.core.server;

import org.jetbrains.annotations.NotNull;
import org.monora.coolsocket.core.CoolSocket;
import org.monora.coolsocket.core.config.ConfigFactory;
import org.monora.coolsocket.core.session.Channel;

import java.net.ServerSocket;
import java.net.Socket;

/**
 * This class handles a server session by accepting connections.
 * <p>
 * After it receives a connection request via {@link ServerSocket#accept()}, it will configure the client socket using
 * {@link ConfigFactory#configureClient(Socket)} and let
 * {@link ConnectionManager#handleClient(CoolSocket, Channel)} handle the rest of the operation.
 */
public interface ServerExecutor {
    /**
     * Handle a server session by accepting connections and assigning them to executors. The server socket is created
     * outside of the thread to handle startup errors during the start operation.
     *
     * @param coolSocket        That owns the session.
     * @param configFactory     That produces the server socket and configures the sockets.
     * @param connectionManager The connection manager instance that will assign client connections to threads
     *                          and manage the process.
     * @param serverSocket      The server socket that will accept the connections.
     * @throws Exception If an unrecoverable error occurs (e.g., creating a server socket fails).
     */
    void onSession(@NotNull CoolSocket coolSocket, @NotNull ConfigFactory configFactory,
                   @NotNull ConnectionManager connectionManager, @NotNull ServerSocket serverSocket) throws Exception;
}
