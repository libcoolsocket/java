package com.genonbeta.coolsocket;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CoolSocket is TCP socket server with a set of features like keep-alive connections, header
 * support, connection management etc. It only uses native libraries and JSON to do its job.
 */
public abstract class CoolSocket
{
    public static final String TAG = CoolSocket.class.getSimpleName();

    public static final int
            NO_TIMEOUT = 0,
            HEADER_MAX_LENGTH = Short.MAX_VALUE;

    public static final String HEADER_ITEM_LENGTH = "length";

    private final ArrayList<ActiveConnection> mConnections = new ArrayList<>();
    private ExecutorService mExecutor;
    private Thread mServerThread;
    private ServerSocket mServerSocket;
    private SocketAddress mSocketAddress = null;
    private int mSocketTimeout = NO_TIMEOUT; // no timeout
    private int mMaxConnections = 10;
    private ServerRunnable mSocketRunnable = new ServerRunnable();

    /**
     * Creates a CoolSocket instance.
     */
    public CoolSocket()
    {
    }

    /**
     * Creates a CoolSocket instance that will be available to the local machine.
     *
     * @param port Port that CoolSocket will run on. A neutral zero would mean any port that is available.
     * @see CoolSocket#setSocketAddress(SocketAddress)
     */
    public CoolSocket(int port)
    {
        mSocketAddress = new InetSocketAddress(port);
    }

    /**
     * Creates a CoolSocket instance that will be available to an address range.
     *
     * @param address IPv4 address for network interface.
     * @param port    Port that CoolSocket will run on. A neutral zero would mean any port that is available.
     * @see CoolSocket#setSocketAddress(SocketAddress)
     */
    public CoolSocket(String address, int port)
    {
        mSocketAddress = new InetSocketAddress(address, port);
    }

    /**
     * When a client is connected, this method will be called.
     *
     * @param activeConnection The connection object that represents the client.
     */
    protected abstract void onConnected(ActiveConnection activeConnection);

    /**
     * Connect to a CoolSocket server.
     *
     * @param handler The handler that will handle the connection.
     */
    public static void connect(final Client.ConnectionHandler handler)
    {
        new Thread(() -> handler.onConnect(new Client())).start();
    }

    /**
     * Counts the total connection of a client to the CoolSocket server.
     *
     * @param address Client address.
     * @return The total number of connections.
     */
    public int getConnectionCountByAddress(InetAddress address)
    {
        int returnObject = 0;

        for (ActiveConnection activeConnection : getConnections())
            if (activeConnection.getAddress().equals(address))
                returnObject++;

        return returnObject;
    }

    /**
     * Returns the active connections to the server. You should copy the members if the task you
     * need to carry out will take long. The copying process should be synchronized to prevent
     * errors.
     *
     * @return The original list instance that holds the connections objects.
     */
    public synchronized List<ActiveConnection> getConnections()
    {
        return mConnections;
    }

    /**
     * Returns the executor that starts child threads as needed. The child threads are used to allow
     * simultaneous connections.
     *
     * @return The executor.
     */
    public ExecutorService getExecutor()
    {
        if (mExecutor == null)
            mExecutor = Executors.newFixedThreadPool(mMaxConnections);

        return mExecutor;
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
        return getServerSocket().getLocalPort();
    }

    /**
     * @return The server socket object.
     */
    protected ServerSocket getServerSocket()
    {
        return mServerSocket;
    }

    /**
     * @return The server thread.
     */
    protected Thread getServerThread()
    {
        return mServerThread;
    }

    /**
     * This returns the socket address that is used to run the server.
     *
     * @return Null if nothing was assigned before the original instance
     * of the socket address.
     */
    public SocketAddress getSocketAddress()
    {
        return mSocketAddress;
    }

    /**
     * @return The socket runnable.
     */
    protected ServerRunnable getSocketRunnable()
    {
        return mSocketRunnable;
    }

    /**
     * Returns The timeout defines the time before the server gives up waiting for receiving or sending the
     * response.
     *
     * @return The timeout in millisecond. If not defined previously, it might be {@link CoolSocket#NO_TIMEOUT}.
     */
    public int getSocketTimeout()
    {
        return mSocketTimeout;
    }

    /**
     * This checks whether there is a server thread and if it is running
     *
     * @return False if the thread is not running, true otherwise.
     */
    public boolean isInterrupted()
    {
        return getServerThread() == null || getServerThread().isInterrupted();
    }

    public boolean isListening()
    {
        return getServerSocket() != null && getServerSocket().isBound() && !getServerSocket().isClosed();
    }

    /**
     * This checks if the server thread is alive.
     *
     * @return True if it is.
     */
    public boolean isServerAlive()
    {
        return getServerThread() != null && getServerThread().isAlive();
    }

    /**
     * When a client is connected, to not block the server thread, we call this method to communicate
     * with it. The socket is different from a normal socket connection where the data should also
     * contain a header.
     *
     * @param socket The socket that is connected to the client.
     */
    protected void respondRequest(final Socket socket)
    {
        if (mMaxConnections > 0 && getConnections().size() >= mMaxConnections)
            return;

        getExecutor().submit(() -> {
            ActiveConnection connection = null;

            try (ActiveConnection activeConnection = new ActiveConnection(socket, mSocketTimeout)) {
                if (mSocketTimeout > NO_TIMEOUT)
                    activeConnection.getSocket().setSoTimeout(mSocketTimeout);

                synchronized (mConnections) {
                    mConnections.add(activeConnection);
                }

                connection = activeConnection;
                onConnected(activeConnection);
            } catch (Exception e) {
                onInternalError(e);
            } finally {
                if (connection != null)
                    synchronized (mConnections) {
                        mConnections.remove(connection);
                    }
            }
        });
    }

    /**
     * Replaces the executor.
     *
     * @param executor The executor that will handle starting threads for requests and connections.
     */
    public void setExecutor(ExecutorService executor)
    {
        mExecutor = executor;
    }

    /**
     * Defines the maximum connection number that the server is allowed to handle at the same time.
     * There is always a limit for safety purposes {@link CoolSocket#mMaxConnections}.
     *
     * @param value A positive number to limit the maximum allowed connections.
     */
    public void setMaxConnections(int value)
    {
        mMaxConnections = value;
    }

    /**
     * Replaces the socket address that the server will run on.
     *
     * @param address The address that is used to start the server.
     */
    public void setSocketAddress(SocketAddress address)
    {
        mSocketAddress = address;
    }

    /**
     * Sets the timeout that limits the time CoolSocket waits for the response delivery. The limit is
     * between the last time a byte arrives and the current time, not the time request was made.
     *
     * @param timeout Time in millisecond.
     * @see CoolSocket#getSocketTimeout()
     */
    public void setSocketTimeout(int timeout)
    {
        mSocketTimeout = timeout;
    }

    /**
     * Restarts the server ensuring it is ready for next connections.
     *
     * @param timeout The time in milliseconds
     * @return False if the server is running and did not stop or failed to start. True
     * if the server will be started{@link CoolSocket#start()}
     * @see CoolSocket#start()
     * @see CoolSocket#start(int)
     * @see CoolSocket#onServerStarted()
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
     * Starts the server.
     *
     * @return Returns if the server will be started.
     * @see CoolSocket#restart(int)
     * @see CoolSocket#start(int)
     * @see CoolSocket#onServerStarted()
     */
    public boolean start()
    {
        if (getServerSocket() == null || getServerSocket().isClosed()) {
            try {
                mServerSocket = new ServerSocket();
                getServerSocket().bind(mSocketAddress);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        if (!isServerAlive()) {
            mServerThread = new Thread(getSocketRunnable());

            getServerThread().setDaemon(true);
            getServerThread().setName(TAG + " Main Thread");
        } else if (getServerThread().isAlive())
            return false;

        getServerThread().start();

        return true;
    }

    /**
     * Starts the server ensuring the server socket has started listening.
     *
     * @param timeout The time in milliseconds
     * @return True if the server is started and ready to accept new connections.
     * @see CoolSocket#start()
     * @see CoolSocket#restart(int)
     * @see CoolSocket#onServerStarted()
     */
    public boolean start(int timeout)
    {
        if (!isServerAlive())
            start();

        if (isListening())
            return true;

        if (timeout <= 0)
            throw new IllegalStateException("Can't supply timeout as zero");

        double timeoutAt = System.nanoTime() + timeout * 1e6;
        while (System.nanoTime() < timeoutAt)
            if (isListening())
                return true;

        return false;
    }

    /**
     * Stops the CoolSocket server.
     *
     * @return True if the server was running and has been stopped.
     */
    public boolean stop()
    {
        if (isInterrupted())
            return false;

        getServerThread().interrupt();

        if (!getServerSocket().isClosed()) {
            try {
                getServerSocket().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return true;
    }

    /**
     * Override this method to be informed when the server is started.
     */
    public void onServerStarted()
    {
    }

    /**
     * Override this method to be informed when the server is stopped.
     */
    public void onServerStopped()
    {
    }

    /**
     * This method will be called when the connection handling failed or something that was not
     * expected happened.
     *
     * @param exception The error that occurred.
     */
    public void onInternalError(Exception exception)
    {
        exception.printStackTrace();
    }

    /**
     * Handles the running process of the server.
     */
    private class ServerRunnable implements Runnable
    {
        @Override
        public void run()
        {
            try {
                onServerStarted();

                do {
                    Socket request = CoolSocket.this.getServerSocket().accept();

                    if (CoolSocket.this.isInterrupted())
                        request.close();
                    else
                        respondRequest(request);
                }
                while (!CoolSocket.this.isInterrupted());
            } catch (IOException e) {
                if (!isInterrupted())
                    CoolSocket.this.onInternalError(e);
            } finally {
                onServerStopped();
            }
        }
    }

    /**
     * This class helps the connection process to a CoolSocket server.
     */
    public static class Client
    {
        private Object mReturn;

        /**
         * Connects to a CoolSocket server.
         *
         * @param socketAddress    The server address.
         * @param operationTimeout The timeout before the operation is declared to have failed.
         * @return The connection object.
         * @throws IOException When the connection fails to establish.
         */
        public ActiveConnection connect(SocketAddress socketAddress, int operationTimeout) throws IOException
        {
            return new ActiveConnection(operationTimeout).connect(socketAddress);
        }

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

        /**
         * A class that emulates a connection to a CoolSocket server.
         */
        public interface ConnectionHandler
        {
            /**
             * There is nothing special about this. It just returns a {@link Client} object. But it
             * is here in the hope that the client will do some special stuff before the it is used
             * by the target. To do so, you should override this class and do some magic before
             * calling this method.
             *
             * @param client The client that is used to connect.
             */
            void onConnect(Client client);
        }
    }
}