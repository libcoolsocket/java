package com.genonbeta.coolsocket;

import com.genonbeta.coolsocket.server.ConnectionManager;
import com.genonbeta.coolsocket.server.DefaultServerExecutorFactory;
import com.genonbeta.coolsocket.server.ServerExecutor;
import com.genonbeta.coolsocket.server.ServerExecutorFactory;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.logging.Logger;

/**
 * CoolSocket is a TCP socket implementation for various platforms aiming to be fast, easy to work on.
 */
public abstract class CoolSocket
{
    public static final int NO_TIMEOUT = 0;

    public static final int
            FLAG_NONE = 0,
            FLAG_DATA_CHUNKED = 1 << 1; // Chunked if not single

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
     * This should not be called before the {@link CoolSocket#start()} is called.
     * If the server was started with random port, this returns the port
     * assigned to the server.
     *
     * @return The port that the server is running on.
     */
    public int getLocalPort()
    {
        return inSession() ? getSession().;
    }

    /**
     * The session that is still accepting requests and hasn't been interrupted or still waiting to exit. This may
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

    public boolean isListening()
    {
        throw new NotImplementedException();
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
     * When a client is connected, to not block the server thread, we call this method to communicate
     * with it. The socket is different from a normal socket connection where the data should also
     * contain a header.
     *
     * @param socket The socket that is connected to the client.
     */
    public void respondRequest(final Socket socket)
    {
        connectionManager.handleClient(this, new ActiveConnection(socket));
    }

    /**
     * Restarts the server ensuring it is ready for next connections.
     *
     * @param timeout The time in milliseconds
     * @return False if the server is running and did not stop or failed to start. True
     * if the server will be started{@link CoolSocket#start()}
     * @see CoolSocket#start()
     * @see CoolSocket#start(int)
     */
    public boolean restart(int timeout)
    {
        if (timeout <= 0)
            throw new IllegalStateException("Can't supply timeout as zero");

        if (isServerAlive())
            if (!stop())
                return false;

        double timeoutAt = System.nanoTime() + timeout * 1e6;
        while (System.nanoTime() < timeoutAt)
            if (!isListening() && !isServerAlive())
                return start(timeout);

        return false;
    }

    /**
     * Start listening for connections.
     *
     * @return the session which represents the listening session.
     * @throws IOException if an unrecoverable error occurs.
     */
    public Session startAsynchronously() throws IOException
    {
        if (isListening())
            throw new IllegalStateException("The server is already running.");

        ServerSocket serverSocket = getConfigFactory().createServer();
        ServerExecutor serverExecutor = getServerExecutorFactory().createServerExecutor();

        Session session = new Session(serverSocket, serverExecutor);
        session.startSession();

        return session;
    }

    public void start() throws IOException
    {

    }


    /**
     * Stops the CoolSocket server.
     *
     * @return True if the server was running and has been stopped.
     */
    public boolean stop()
    {


        return true;
    }

    /**
     * This class helps the connection process to a CoolSocket server.
     */
    public static class Client
    {
        private Object mReturn;

        /**
         * This emulates the connection to a server.
         *
         * @param connection    The connection object.
         * @param socketAddress The address that should be connected.
         * @throws IOException When the connection fails.
         */
        public void connect(ActiveConnection connection, SocketAddress socketAddress) throws IOException
        {
            connection.connect(socketAddress);
        }

        /**
         * You can return object when the process exits. This is mostly useful when the same thread
         * is used for the connection.
         *
         * @return The object given during the connection. Null if nothing was given.
         * @see Client#setReturn(Object)
         */
        public Object getReturn()
        {
            return mReturn;
        }

        /**
         * This sets the object that should be returned during the connection process.
         *
         * @param returnedObject The object to return.
         * @see Client#getReturn()
         */
        public void setReturn(Object returnedObject)
        {
            mReturn = returnedObject;
        }
    }

    /**
     * The class that holds the server related data for an active session.
     */
    private class Session implements Runnable
    {
        /**
         * The thread that will run this session.
         */
        public Thread serverThread;

        public ServerSocket serverSocket;

        public ServerExecutor serverExecutor;

        /**
         * A session can only run once. This field ensures that.
         */
        private boolean started = false;

        /**
         * Whether this session has started listening or exited.
         */
        private boolean listening = false;

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
            this.serverSocket = serverSocket;
            this.serverExecutor = serverExecutor;
        }

        public ServerExecutor getServerExecutor()
        {
            return serverExecutor;
        }

        public ServerSocket getServerSocket()
        {
            return serverSocket;
        }

        public Thread getServerThread()
        {
            return serverThread;
        }

        public boolean isStarted()
        {
            return started;
        }

        public boolean isListening()
        {
            return listening;
        }

        private void startSession()
        {
            if (started)
                throw new IllegalStateException("This session has already been run.");

            started = true;
            serverThread = new Thread(this);

            serverThread.start();
        }

        @Override
        public void run()
        {
            if (!started)
                throw new IllegalStateException("Session.started cannot be false. Please ensure Session runnable " +
                        " starts itself with startSession() method so that it can ensure a safe usage.");

            listening = true;

            try {
                serverExecutor.onSession(CoolSocket.this, getConfigFactory(), serverSocket);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                CoolSocket.this.serverSession = null;
                listening = false;
            }
        }
    }
}