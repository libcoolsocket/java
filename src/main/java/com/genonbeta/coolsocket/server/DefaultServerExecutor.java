package com.genonbeta.coolsocket.server;

import com.genonbeta.coolsocket.ConfigFactory;
import com.genonbeta.coolsocket.CoolSocket;

import java.net.ServerSocket;
import java.util.logging.Level;

public class DefaultServerExecutor implements ServerExecutor
{
    @Override
    public void onSession(CoolSocket coolSocket, ConfigFactory configFactory, ServerSocket serverSocket)
    {
        do {
            try {
                coolSocket.respondRequest(serverSocket.accept());
            } catch (Exception e) {
                coolSocket.getLogger().log(Level.SEVERE, "The server returned an error.", e);
            }
        }
        while (!Thread.currentThread().isInterrupted());
    }
}
