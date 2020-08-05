package org.monora.coolsocket.core.server;

import org.monora.coolsocket.core.config.ConfigFactory;
import org.monora.coolsocket.core.CoolSocket;

import java.net.ServerSocket;

public interface ServerExecutor
{
    /**
     * Handle a server session by accepting connections and assigning them to executors. The server socket is created
     * outside of the thread to handle startup errors during the start operation.
     *
     * @param coolSocket    That owns the session.
     * @param configFactory That produces the server socket and configures the sockets.
     * @param serverSocket  The server socket that will accept the connections.
     * @throws Exception If an unrecoverable error occurs (e.g., creating a server socket fails).
     */
    void onSession(CoolSocket coolSocket, ConfigFactory configFactory, ServerSocket serverSocket) throws Exception;
}
