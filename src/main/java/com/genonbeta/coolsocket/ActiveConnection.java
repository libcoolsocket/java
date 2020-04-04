package com.genonbeta.coolsocket;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeoutException;

import static com.genonbeta.coolsocket.CoolSocket.HEADER_ITEM_LENGTH;
import static com.genonbeta.coolsocket.CoolSocket.NO_TIMEOUT;

/**
 * This class represents a connection to CoolServer client and server.
 */
public class ActiveConnection implements Closeable
{
    public static final String TAG = ActiveConnection.class.getSimpleName();

    private Socket mSocket;
    private int mTimeout = NO_TIMEOUT;
    private int mId = getClass().hashCode();

    /**
     * An instance that uses its own socket. Call {@link ActiveConnection(Socket)} with null
     * argument if the socket will be provided later.
     */
    public ActiveConnection()
    {
        this(new Socket());
    }

    /**
     * An instance that uses its own socket with a timeout. Call {@link ActiveConnection(Socket)}
     * with null argument if the socket will be provided later.
     *
     * @param timeout Timeout that will limit the amount of time that the requests to wait for
     *                another packet to arrive or go.
     */
    public ActiveConnection(int timeout) throws SocketException
    {
        this(new Socket(), timeout);
    }

    /**
     * An instance with socket connection to a CoolSocket server.
     *
     * @param socket The connection to CoolSocket server or client.
     */
    public ActiveConnection(Socket socket)
    {
        mSocket = socket;
    }

    /**
     * An instance with timeout and socket connection to a CoolSocket server.
     *
     * @param socket  The connection to CoolSocket server or client.
     * @param timeout Timeout that will limit the amount of time that the requests to wait for
     *                another packet to arrive or go.
     */
    public ActiveConnection(Socket socket, int timeout) throws SocketException
    {
        this(socket);
        setTimeout(timeout);
    }

    @Override
    public void close() throws IOException
    {
        if (mSocket != null)
            mSocket.close();
    }

    /**
     * Connects to a CoolSocket server.
     *
     * @param socketAddress The address of CoolSocket server.
     * @return Returns the instance of this class.
     * @throws IOException When connection fails.
     */
    public ActiveConnection connect(SocketAddress socketAddress) throws IOException
    {
        getSocket().bind(null);
        getSocket().connect(socketAddress);

        return this;
    }

    /**
     * This ensures the connection is closed before this instance of the class is about to be
     * destroyed.
     *
     * @throws Throwable Override to use this feature.
     * @deprecated by the parent
     */
    @Override
    @Deprecated
    protected void finalize() throws Throwable
    {
        super.finalize();

        if (getSocket() != null && !getSocket().isClosed()) {
            System.out.println(TAG + ": Connections should be closed before their references are being destroyed");
            getSocket().close();
        }
    }

    /**
     * This should be called after ensuring that the socket is provided
     *
     * @return The address that the socket is bound to.
     */
    public InetAddress getAddress()
    {
        return getSocket().getInetAddress();
    }

    /**
     * This should be called after ensuring that the socket is provided.
     *
     * @return The readable address the socket is bound to.
     */
    public String getClientAddress()
    {
        return getAddress().getHostAddress();
    }

    /**
     * A proposed method to determine a connection with a unique id.
     *
     * @return The class id defined during creation of this instance of the class.
     * @see ActiveConnection#setId(int)
     */
    public int getId()
    {
        return mId;
    }

    /**
     * The socket that is used to communicate
     *
     * @return Null if no socket was provided or the socket instance.
     */
    public Socket getSocket()
    {
        return mSocket;
    }

    /**
     * On server side, this is defined with the given server timeout by default. When used on client
     * side, it is defined by the method associated with it.
     *
     * @return Timeout in milliseconds.
     */
    public int getTimeout()
    {
        return mTimeout;
    }

    /**
     * This uses {@link Object#toString()} method which will return the unique id to this instance of
     * the class to determine whether they are the same instance.
     *
     * @param obj An instance of this class is expected and if not then the parent class will handle it.
     * @return True if the two object is the same.
     */
    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof ActiveConnection ? obj.toString().equals(toString()) : super.equals(obj);
    }

    /**
     * When the opposite side calls {@link ActiveConnection#reply(String)}, this method should
     * be invoked so that the communication occurs. The order of the calls should be in order
     * with their peers. For instance, when you call {@link ActiveConnection#reply(String)} method,
     * the other side should already have called this method or vice versa, which means asynchronous
     * task should not block the thread when the data is being transferred.
     *
     * @return The response that is received.
     * @throws IOException      When a socket IO error occurs.
     * @throws TimeoutException When the amount time exceeded while waiting for another byte to
     *                          transfer.
     * @throws JSONException    When the JSON parsing error occurs.
     * @see #reply(String)
     * @see #reply(String, JSONObject)
     */
    public synchronized Response receive() throws IOException, TimeoutException, JSONException
    {
        InputStream inputStream = getSocket().getInputStream();
        ByteArrayOutputStream header = new ByteArrayOutputStream();
        ByteArrayOutputStream index = new ByteArrayOutputStream();

        final byte[] buffer = new byte[8096];
        final int headerMaxLength = 2;
        JSONObject headerAsJson = null;
        int headerLength = -1;
        int indexLength = -1;
        int len;
        int offset = 0;
        int readAsMuch = headerMaxLength; // first get the 16-bit header data length
        long lastRead = System.nanoTime();

        while (indexLength == -1 || index.size() < indexLength) {
            if ((len = inputStream.read(buffer, offset, readAsMuch)) > 0) {
                lastRead = System.nanoTime();

                if (headerLength < 0) {
                    if (len + offset == headerMaxLength) {
                        headerLength = ByteBuffer.wrap(buffer, 0, headerMaxLength).getShort();
                        if (headerLength <= 0)
                            break;
                    } else
                        offset = headerMaxLength - len;
                } else if (indexLength < 0) {
                    header.write(buffer, 0, len);
                    header.flush();

                    if (header.size() == headerLength) {
                        headerAsJson = new JSONObject(header.toString());
                        indexLength = headerAsJson.getInt(HEADER_ITEM_LENGTH);

                        if (indexLength <= 0)
                            break;
                    }
                } else {
                    index.write(buffer, 0, len);
                    index.flush();
                }

                if (headerLength > -1) {
                    offset = 0;

                    if (indexLength < 0) {
                        readAsMuch = Math.min(headerLength - header.size(), buffer.length);
                    } else {
                        readAsMuch = Math.min(indexLength - index.size(), buffer.length);
                    }
                }
            }

            if (getTimeout() > NO_TIMEOUT && System.nanoTime() - lastRead > getTimeout() * 1e6)
                throw new TimeoutException("Read timed out!");
        }

        return new Response(getSocket().getRemoteSocketAddress(), headerAsJson, index.toString(), indexLength);
    }

    /**
     * This will send reply to the other side with no extra header data.
     *
     * @see #reply(String, JSONObject)
     */
    public void reply(String out) throws TimeoutException, IOException, JSONException
    {
        reply(out, new JSONObject());
    }

    /**
     * This will send the given data to the other side while the other side has already called
     * {@link ActiveConnection#receive()}.
     *
     * @param out    The data that should be sent.
     * @param header the header that will be sent to the client with the length of the data and other data depending
     *               on your needs
     * @throws IOException      When a socket IO error occurs.
     * @throws TimeoutException When the amount time exceeded while waiting for another byte to
     *                          transfer.
     * @throws JSONException    When the JSON parsing error occurs.
     * @see ActiveConnection#receive()
     */
    public synchronized void reply(String out, JSONObject header) throws TimeoutException, IOException, JSONException
    {
        OutputStream outputStream = getSocket().getOutputStream();
        byte[] outputBytes = out == null ? new byte[0] : out.getBytes();
        String headerString = header.put(HEADER_ITEM_LENGTH, outputBytes.length).toString();

        // The first 16 bit data represents length of the header
        if (headerString.length() > CoolSocket.HEADER_MAX_LENGTH)
            throw new IllegalStateException("The maximum length of a header can be " + (CoolSocket.HEADER_MAX_LENGTH));

        outputStream.write(ByteBuffer.allocate(2).putShort((short) headerString.length()).array());
        outputStream.flush();
        outputStream.write(headerString.getBytes());
        outputStream.flush();

        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputBytes);

        byte[] buffer = new byte[8096];
        int len;

        while ((len = inputStream.read(buffer)) != -1) {
            long writeStart = System.nanoTime();

            outputStream.write(buffer, 0, len);
            outputStream.flush();

            if (getTimeout() > NO_TIMEOUT && System.nanoTime() - writeStart > getTimeout() * 1e6)
                throw new TimeoutException("Operation timed out!");
        }
    }

    /**
     * This ID for this instance of CoolSocket can be used to identify it.
     *
     * @param id that identifies this instance
     */
    public void setId(int id)
    {
        mId = id;
    }

    /**
     * Sets the timeout
     *
     * @param timeout The timout in milliseconds.
     * @see ActiveConnection#getTimeout()
     */
    public void setTimeout(int timeout) throws SocketException
    {
        if (timeout < 0)
            throw new NumberFormatException("Timeout value can only be >= 0");

        mTimeout = timeout;
        getSocket().setSoTimeout(timeout);
    }
}