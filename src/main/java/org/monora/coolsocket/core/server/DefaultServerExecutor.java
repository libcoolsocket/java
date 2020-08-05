package org.monora.coolsocket.core.server;

import org.monora.coolsocket.core.config.ConfigFactory;
import org.monora.coolsocket.core.CoolSocket;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;

public class DefaultServerExecutor implements ServerExecutor
{
    @Override
    public void onSession(CoolSocket coolSocket, ConfigFactory configFactory, ServerSocket serverSocket)
            throws IOException
    {
        do {
            try {
                coolSocket.respondRequest(serverSocket.accept());
            } catch (SocketException e) {
                coolSocket.getLogger().info("Server socket exited.");
            }
        }
        while (!Thread.currentThread().isInterrupted());
    }
}
