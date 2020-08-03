package com.genonbeta.coolsocket.server;

import com.genonbeta.coolsocket.ConfigFactory;
import com.genonbeta.coolsocket.CoolSocket;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.logging.Level;

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
