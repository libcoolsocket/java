package org.monora.coolsocket.core.server;

import org.monora.coolsocket.core.CoolSocket;
import org.monora.coolsocket.core.config.ConfigFactory;
import org.monora.coolsocket.core.session.ActiveConnection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class DefaultServerExecutor implements ServerExecutor
{
    @Override
    public void onSession(CoolSocket coolSocket, ConfigFactory configFactory, ConnectionManager connectionManager,
                          ServerSocket serverSocket)
            throws IOException
    {
        do {
            try {
                Socket clientSocket = serverSocket.accept();
                ActiveConnection activeConnection = configFactory.configureClient(clientSocket);
                connectionManager.handleClient(coolSocket, activeConnection);
            } catch (SocketException e) {
                coolSocket.getLogger().info("Server socket exited.");
            }
        }
        while (!Thread.currentThread().isInterrupted());
    }
}
