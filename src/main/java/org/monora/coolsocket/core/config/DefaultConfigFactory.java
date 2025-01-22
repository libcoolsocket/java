package org.monora.coolsocket.core.config;

import org.jetbrains.annotations.NotNull;
import org.monora.coolsocket.core.session.Channel;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * The default factory implementation.
 */
public class DefaultConfigFactory implements ConfigFactory {
    /**
     * The socket address.
     */
    private @NotNull SocketAddress socketAddress;

    /**
     * The time window to wait until for a new connection before exiting.
     */
    private int acceptTimeout;

    /**
     * The time window to wait until a new byte arrives before exiting.
     */
    private int readTimeout;

    /**
     * Creates a new instance.
     *
     * @param socketAddress To bind to.
     * @param acceptTimeout The time window to wait until for a new connection before exiting.
     * @param readTimeout   The time window to wait until a new byte arrives before exiting.
     */
    public DefaultConfigFactory(@NotNull SocketAddress socketAddress, int acceptTimeout, int readTimeout) {
        this.socketAddress = socketAddress;
        setAcceptTimeout(acceptTimeout);
        setReadTimeout(readTimeout);
    }

    /**
     * Configure the server socket.
     *
     * @param serverSocket To be configured.
     * @throws IOException If an IO error occurs while configuring.
     */
    @Override
    public void configureServer(@NotNull ServerSocket serverSocket) throws IOException {
        serverSocket.bind(socketAddress);
        serverSocket.setSoTimeout(acceptTimeout);
    }

    /**
     * Configure the socket.
     *
     * @param socket To configure.
     * @return The new channel wrapping the socket.
     * @throws IOException If an IO error occurs while configuring.
     */
    @Override
    public @NotNull Channel configureClient(@NotNull Socket socket) throws IOException {
        socket.setSoTimeout(readTimeout);
        return Channel.wrap(socket);
    }

    /**
     * Create a pre-configured server socket.
     *
     * @return The pre-configured server socket.
     * @throws IOException If an IO error occurs while creating or configuring the server socket.
     */
    @Override
    public @NotNull ServerSocket createServer() throws IOException {
        ServerSocket serverSocket = new ServerSocket();
        configureServer(serverSocket);
        return serverSocket;
    }

    /**
     * The socket address bind to.
     *
     * @return The socket address.
     */
    @Override
    public @NotNull SocketAddress getSocketAddress() {
        return socketAddress;
    }

    /**
     * Set the accept timeout.
     *
     * @param milliSeconds The max time to wait in milliseconds.
     */
    @Override
    public void setAcceptTimeout(int milliSeconds) {
        this.acceptTimeout = milliSeconds;
    }

    /**
     * Set the read timeout.
     *
     * @param milliSeconds The max time to wait in milliseconds.
     */
    @Override
    public void setReadTimeout(int milliSeconds) {
        this.readTimeout = milliSeconds;
    }

    /**
     * Set the socket address to bind.
     *
     * @param socketAddress To be used with server socket.
     */
    @Override
    public void setSocketAddress(@NotNull SocketAddress socketAddress) {
        this.socketAddress = socketAddress;
    }
}
