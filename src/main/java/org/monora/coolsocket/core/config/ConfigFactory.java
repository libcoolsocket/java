package org.monora.coolsocket.core.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;

public interface ConfigFactory
{
    /**
     * Configure the server socket for the server side before the usage. Throws whatever the error it faces due to a
     * misconfiguration.
     *
     * @param serverSocket to be configured.
     * @throws IOException when an unrecoverable error occurs due to misconfiguration.
     */
    void configureServer(ServerSocket serverSocket) throws IOException;

    /**
     * Produce a {@link ServerSocket} instance preconfigured with {@link #configureServer(ServerSocket)}. It is up to
     * you make whether the server socket will be a part of a SSL context.
     *
     * @return a preconfigured server socket instance.
     * @throws IOException if the factory fails to create the server socket instance due to misconfiguration.
     */
    ServerSocket createServer() throws IOException;

    /**
     * Configure the socket connection to a client before its actual usage. The configuration may be different from
     * that of {@link ServerSocket#accept()} assigns.
     *
     * @param client to configure.
     * @throws SocketException when an unrecoverable error occurs due to misconfiguration.
     */
    void configureClient(Socket client) throws SocketException;

    /**
     * The address that the upcoming products will be assigned to. This does not necessarily reflect the
     * @return the address that the server will be bound to.
     */
    SocketAddress getSocketAddress();

    /**
     * Get the server port assigned to the server sockets coming out of this factory. This does not necessarily reflect
     * the ports assigned previous products.
     *
     * @return the port number assigned by this factory instance.
     */
    default int getPort()
    {
        return getSocketAddress() instanceof InetSocketAddress ? ((InetSocketAddress) getSocketAddress()).getPort() : 0;
    }

    /**
     * Time to wait for each client before throwing an error, {@link java.util.concurrent.TimeoutException} in the case
     * case {@link ServerSocket#accept()}.
     *
     * @param timemillis the max time to wait in milliseconds.
     * @see ServerSocket#setSoTimeout(int)
     * @see ServerSocket#accept()
     */
    void setAcceptTimeout(int timemillis);

    /**
     * Read timeout in any scenario. This doesn't affect existing instances. A "0" (zero) value will mean to wait
     * indefinitely.
     *
     * @param timemillis the max time to wait in milliseconds.
     * @see Socket#setSoTimeout(int)
     * @see InputStream#read()
     */
    void setReadTimeout(int timemillis);

    /**
     * Set socket address for the server.
     *
     * @param socketAddress to be used with server socket.
     */
    void setSocketAddress(SocketAddress socketAddress);
}
