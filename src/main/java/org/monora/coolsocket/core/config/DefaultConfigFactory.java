package org.monora.coolsocket.core.config;

import org.monora.coolsocket.core.session.ActiveConnection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;

public class DefaultConfigFactory implements ConfigFactory
{
    private SocketAddress socketAddress;
    private int acceptTimeout;
    private int readTimeout;

    public DefaultConfigFactory(SocketAddress socketAddress, int acceptTimeout, int readTimeout)
    {
        setSocketAddress(socketAddress);
        setAcceptTimeout(acceptTimeout);
        setReadTimeout(readTimeout);
    }

    @Override
    public void configureServer(ServerSocket serverSocket) throws IOException
    {
        serverSocket.bind(socketAddress);
        serverSocket.setSoTimeout(acceptTimeout);
    }

    @Override
    public ActiveConnection configureClient(Socket socket) throws SocketException
    {
        return new ActiveConnection(socket, readTimeout);
    }

    @Override
    public ServerSocket createServer() throws IOException
    {
        ServerSocket serverSocket = new ServerSocket();
        configureServer(serverSocket);
        return serverSocket;
    }

    @Override
    public SocketAddress getSocketAddress()
    {
        return socketAddress;
    }

    @Override
    public void setAcceptTimeout(int timemillis)
    {
        this.acceptTimeout = timemillis;
    }

    @Override
    public void setReadTimeout(int timemillis)
    {
        this.readTimeout = timemillis;
    }

    @Override
    public void setSocketAddress(SocketAddress socketAddress)
    {
        this.socketAddress = socketAddress;
    }
}
