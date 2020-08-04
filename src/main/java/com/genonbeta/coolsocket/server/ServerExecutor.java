package com.genonbeta.coolsocket.server;

import com.genonbeta.coolsocket.config.ConfigFactory;
import com.genonbeta.coolsocket.CoolSocket;

import java.net.ServerSocket;

public interface ServerExecutor
{
    /**
     * Handle a server session by accepting connections and assigning them to executors. The server socket is created
     * outside of the thread to handle startup errors during the start operation.
     *
     * @param coolSocket    that owns the session.
     * @param configFactory that produces the server socket and configures the sockets.
     * @param serverSocket  the server socket that will accept the connections.
     * @throws Exception if an unrecoverable error occurs (e.g., creating a server socket fails).
     */
    void onSession(CoolSocket coolSocket, ConfigFactory configFactory, ServerSocket serverSocket) throws Exception;
}
