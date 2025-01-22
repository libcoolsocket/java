package org.monora.coolsocket.core.server;

import org.jetbrains.annotations.NotNull;
import org.monora.coolsocket.core.CoolSocket;
import org.monora.coolsocket.core.config.ConfigFactory;
import org.monora.coolsocket.core.session.Channel;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.logging.Level;

/**
 * The default server executor implementation.
 */
public class DefaultServerExecutor implements ServerExecutor {
    @Override
    public void onSession(@NotNull CoolSocket coolSocket, @NotNull ConfigFactory configFactory,
                          @NotNull ConnectionManager connectionManager, @NotNull ServerSocket serverSocket) {
        do {
            try {
                Socket clientSocket = serverSocket.accept();
                Channel channel = configFactory.configureClient(clientSocket);
                connectionManager.handleClient(coolSocket, channel);
            } catch (SocketException e) {
                coolSocket.getLogger().fine("Server socket exited.");
            } catch (SocketTimeoutException ignored) {
            } catch (Exception e) {
                coolSocket.getLogger().log(Level.SEVERE, "Caught a severe error.", e);
            }
        }
        while (!Thread.currentThread().isInterrupted());
    }
}
