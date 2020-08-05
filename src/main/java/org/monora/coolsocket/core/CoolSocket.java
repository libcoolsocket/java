package org.monora.coolsocket.core;

import org.monora.coolsocket.core.config.ConfigFactory;
import org.monora.coolsocket.core.config.DefaultConfigFactory;
import org.monora.coolsocket.core.server.*;

import java.io.IOException;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CoolSocket is a TCP socket implementation for various platforms aiming to be fast, easy to work on.
 */
public abstract class CoolSocket
{
    /**
     * Disable timeout.
     */
    public static final int NO_TIMEOUT = 0;

    /**
     * The length of the content is unknown (or unspecified). When used with chunked data transmission, the length will
     * be unknown until the data is received in full.
     */
    public static final int LENGTH_UNSPECIFIED = -1;

    private final Logger logger = Logger.getLogger(toString());
    private final ConfigFactory configFactory;
    private ServerExecutorFactory serverExecutorFactory;
    private ConnectionManager connectionManager;
    private Session serverSession;

    /**
     * Create an instance that will use the default config factory.
     *
     * @param port that the server socket will run on. Use "0" to randomly assign to an available port.
     */
    public CoolSocket(int port)
    {
        this(new InetSocketAddress(port));
    }

    /**
     * Create an instance that will use the default config factory.
     *
     * @param address that the server will be assigned to.
     */
    public CoolSocket(SocketAddress address)
    {
        this(new DefaultConfigFactory(address, NO_TIMEOUT, NO_TIMEOUT));
    }

    /**
     * Create an instance with its own config factory.
     *
     * @param configFactory that will produce ServerSocket, and configure sockets.
     */
    public CoolSocket(ConfigFactory configFactory)
    {
        this.configFactory = configFactory;
    }

    /**
     * When a client is connected, this method will be called.
     *
     * @param activeConnection The connection object that represents the client.
     */
    public abstract void onConnected(ActiveConnection activeConnection);

    /**
     * Get the config factory instance that loads settings on to sockets.
     *
     * @return the config factory instance.
     */
    protected ConfigFactory getConfigFactory()
    {
        return configFactory;
    }

    /**
     * Get the connection manager that handles the threads for clients.
     *
     * @return the connection manager instance.
     */
    protected ConnectionManager getConnectionManager()
    {
        if (connectionManager == null)
            connectionManager = new DefaultConnectionManager();
        return connectionManager;
    }

    /**
     * This will return the port server will be or is running on. If the port number is zero (to assign a random port),
     * this will return '0' as expected, however, if the server has been started, then, the returned value will be the
     * port that the server is running on.
     *
     * @return the port that the server is running on.
     */
    public int getLocalPort()
    {
        Session session = getSession();
        return session == null ? getConfigFactory().getPort() : session.getServerSocket().getLocalPort();
    }

    /**
     * The session that is still accepting requests and hasn't been interrupted or still waiting to exit. This will
     * return null if there is no active session.
     *
     * @return the session that runs the server process.
     */
    public Session getSession()
    {
        return serverSession;
    }

    /**
     * @return the server executor factory instance which defaults when there is none.
     */
    public ServerExecutorFactory getServerExecutorFactory()
    {
        if (serverExecutorFactory == null)
            serverExecutorFactory = new DefaultServerExecutorFactory();

        return serverExecutorFactory;
    }

    /**
     * Check whether there is an active session.
     *
     * @return true when there is a session and hasn't exited.
     */
    public boolean inSession()
    {
        return serverSession != null;
    }

    /**
     * Check whether the server is listening for connections.
     *
     * @return true if the server is listening, or false if otherwise.
     */
    public boolean isListening()
    {
        Session session = getSession();
        return session != null && session.isListening();
    }

    /**
     * Get the logger for this CoolSocket instance.
     *
     * @return the logger instance.
     */
    public Logger getLogger()
    {
        return logger;
    }

    /**
     * Handle the request from a client on a different thread.
     *
     * @param socket the socket representing the client.
     * @throws SocketException if configuring the socket with the config factory fails.
     */
    public void respondRequest(final Socket socket) throws SocketException
    {
        getConfigFactory().configureClient(socket);
        getConnectionManager().handleClient(this, new ActiveConnection(socket));
    }

    /**
     * Restart the server without changing anything
     *
     * @param timeout time to wait before giving up.
     * @throws IOException          when something related socket set up goes wrong (e.g., a bind exception).
     * @throws InterruptedException if the calling thread exits while waiting for the lock to release.
     * @see #start()
     * @see #start(long)
     * @see #stop()
     * @see #stop(long)
     */
    public void restart(int timeout) throws IOException, InterruptedException
    {
        if (timeout <= 0)
            throw new IllegalStateException("Can't supply timeout as zero");

        stop(timeout);
        start(timeout);
    }

    /**
     * Start listening for connections.
     *
     * @return the session which represents the listening session.
     * @throws IOException if an unrecoverable error occurs.
     * @see #start()
     * @see #start(long)
     */
    public Session startAsynchronously() throws IOException
    {
        if (isListening())
            throw new IllegalStateException("The server is already running.");

        ServerSocket serverSocket = getConfigFactory().createServer();
        ServerExecutor serverExecutor = getServerExecutorFactory().createServerExecutor();

        Session session = new Session(serverSocket, serverExecutor);
        session.start();

        return session;
    }

    /**
     * Start the server session and ensure it has started when returned, meaning it will block the calling thread until
     * the server starts. For a nonblocking start, use {@link #startAsynchronously()}.
     *
     * @throws IOException          if an error occurs during the set-up process of the server socket.
     * @throws InterruptedException if the calling thread goes into the interrupted state.
     * @see #start(long)
     * @see #startAsynchronously()
     */
    public void start() throws IOException, InterruptedException
    {
        start(0);
    }

    /**
     * Start the server session and ensure it has started in the given timespan or throw an {@link IOException} saying
     * that the server could not start listening.
     *
     * @param timeout time in milliseconds to wait before giving up with an error.
     * @throws IOException          if something related to the set-up process of the server socket goes wrong or the
     *                              server socket cannot start listening in the given time.
     * @throws InterruptedException if the calling thread goes in to the interrupted state.
     */
    public void start(long timeout) throws IOException, InterruptedException
    {
        Session session = startAsynchronously();
        session.waitUntilStateChange(timeout);
        if (!session.isListening())
            throw new IOException("The server could not start listening.");
    }

    /**
     * Stops the CoolSocket server session without blocking the calling thread. This shouldn't be called if there is no
     * session, and it will throw {@link IllegalStateException} if you do so. Ensure you are invoking this when
     * {@link #isListening()} returns true.
     *
     * @return the session object representing the active listening session.
     * @see #stop()
     * @see #stop(long)
     */
    public Session stopAsynchronously()
    {
        if (!isListening())
            throw new IllegalStateException("The server is not running or hasn't started yet. Make sure this call" +
                    " happens during a valid session's lifecycle.");

        Session session = getSession();
        session.interrupt();
        return session;
    }

    /**
     * Stop the active listening session and close all the connections to the clients without a prior notice. This will
     * block the calling thread indefinitely until the lock releases. Use {@link #stopAsynchronously()} to for an
     * asynchronous stop call.
     * <p>
     * This shouldn't be called when there is no session and will throw an {@link IllegalStateException} error if you
     * do so.
     *
     * @throws InterruptedException if the calling thread goes into interrupted state.
     * @see #stop(long)
     * @see #stopAsynchronously()
     */
    public void stop() throws InterruptedException
    {
        Session session = stopAsynchronously();
        session.waitUntilStateChange();
    }

    /**
     * Stop the active listening session and close all the connections to the clients without a prior notice. This will
     * block the calling thread for the given timeout amount (indefinitely if '0') until the lock releases. Use
     * {@link #stopAsynchronously()} for an asynchronous stop operation. This will throw an {@link IOException} if the
     * server fails to stop in time.
     *
     * @param timeout time to wait in millisecond
     * @throws InterruptedException if the calling thread goes into interrupted state.
     * @throws IOException          if an IO error occurs, or the session fails to close.
     * @see #stop()
     * @see #stopAsynchronously()
     */
    public void stop(long timeout) throws InterruptedException, IOException
    {
        Session session = stopAsynchronously();
        session.waitUntilStateChange(timeout);
        if (session.isListening())
            throw new IOException("The server could not stop listening.");
    }

    /**
     * The class that holds the server related data for an active session.
     */
    private class Session extends Thread
    {
        public ServerSocket serverSocket;

        public ServerExecutor serverExecutor;

        private final Object stateLock = new Object();

        private boolean listening;

        /**
         * Create a session with the given object which should only be known to this class. Any object here is useless
         * after the run() method of this class exits. This class assigns itself to the CoolSocket instance that owns
         * it whenever a new session starts and erases itself from it whenever it exits.
         *
         * @param serverSocket   accepting the connections for this server session.
         * @param serverExecutor runs the server with the given data objects in this instance of session.
         */
        public Session(ServerSocket serverSocket, ServerExecutor serverExecutor)
        {
            super("CoolSocket Server Session");

            this.serverSocket = serverSocket;
            this.serverExecutor = serverExecutor;
        }

        private void closeServerSocket()
        {
            if (serverSocket.isClosed())
                return;

            try {
                serverSocket.close();
            } catch (IOException e) {
                if (!isInterrupted())
                    CoolSocket.this.getLogger().info("The server socket was already closed ");
            }
        }

        public ServerExecutor getServerExecutor()
        {
            return serverExecutor;
        }

        public ServerSocket getServerSocket()
        {
            return serverSocket;
        }

        @Override
        public void interrupt()
        {
            super.interrupt();
            closeServerSocket();
        }

        public boolean isListening()
        {
            return listening;
        }

        @Override
        public void run()
        {
            if (Thread.currentThread() != this)
                throw new IllegalStateException("A session is its own thread and should not be run as a runnable.");

            listening = true;
            synchronized (stateLock) {
                stateLock.notifyAll();
            }

            try {
                serverExecutor.onSession(CoolSocket.this, getConfigFactory(), serverSocket);
            } catch (Exception e) {
                if (!isInterrupted())
                    CoolSocket.this.getLogger().log(Level.SEVERE, "Server exited with an unexpected error.", e);
            } finally {
                closeServerSocket();

                CoolSocket.this.serverSession = null;
                listening = false;
                synchronized (stateLock) {
                    stateLock.notifyAll();
                }
            }
        }

        @Override
        public synchronized void start()
        {
            super.start();
            CoolSocket.this.serverSession = this;
        }

        public void waitUntilStateChange() throws InterruptedException
        {
            waitUntilStateChange(0);
        }

        public void waitUntilStateChange(long ms) throws InterruptedException
        {
            synchronized (stateLock) {
                if (ms == 0)
                    stateLock.wait();
                else
                    stateLock.wait(ms);
            }
        }
    }
}