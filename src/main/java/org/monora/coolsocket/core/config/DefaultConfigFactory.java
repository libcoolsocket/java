package org.monora.coolsocket.core.config;

import org.jetbrains.annotations.NotNull;
import org.monora.coolsocket.core.session.ActiveConnection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;

public class DefaultConfigFactory implements ConfigFactory
{
    private @NotNull SocketAddress socketAddress;
    private int acceptTimeout;
    private int readTimeout;

    public DefaultConfigFactory(@NotNull SocketAddress socketAddress, int acceptTimeout, int readTimeout)
    {
        this.socketAddress = socketAddress;
        setAcceptTimeout(acceptTimeout);
        setReadTimeout(readTimeout);
    }

    @Override
    public void configureServer(@NotNull ServerSocket serverSocket) throws IOException
    {
        serverSocket.bind(socketAddress);
        serverSocket.setSoTimeout(acceptTimeout);
    }

    @Override
    public @NotNull ActiveConnection configureClient(@NotNull Socket socket) throws SocketException
    {
        return new ActiveConnection(socket, readTimeout);
    }

    @Override
    public @NotNull ServerSocket createServer() throws IOException
    {
        ServerSocket serverSocket = new ServerSocket();
        configureServer(serverSocket);
        return serverSocket;
    }

    @Override
    public @NotNull SocketAddress getSocketAddress()
    {
        return socketAddress;
    }

    @Override
    public void setAcceptTimeout(int milliSeconds)
    {
        this.acceptTimeout = milliSeconds;
    }

    @Override
    public void setReadTimeout(int milliSeconds)
    {
        this.readTimeout = milliSeconds;
    }

    @Override
    public void setSocketAddress(@NotNull SocketAddress socketAddress)
    {
        this.socketAddress = socketAddress;
    }
}
