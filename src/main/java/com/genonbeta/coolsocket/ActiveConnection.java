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

import static com.genonbeta.coolsocket.CoolSocket.FLAG_NONE;
import static com.genonbeta.coolsocket.CoolSocket.NO_TIMEOUT;

/**
 * This class represents a connection to CoolServer client and server.
 */
public class ActiveConnection implements Closeable
{
    public static final String TAG = ActiveConnection.class.getSimpleName();

    private final Socket mSocket;

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
        socket.setSoTimeout(timeout);
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
     * The socket that is used to communicate
     *
     * @return Null if no socket was provided or the socket instance.
     */
    public Socket getSocket()
    {
        return mSocket;
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

    private ByteBuffer readAsByteBuffer(InputStream inputStream, byte[] buffer, int offset, int length)
            throws IOException, TimeoutException
    {
        return ByteBuffer.wrap(read(inputStream, buffer, length), offset, length);
    }

    private byte[] read(InputStream inputStream, byte[] buffer, int length) throws IOException, TimeoutException
    {
        if (buffer.length < length)
            throw new IOException("Buffer cannot smaller than the byte size that is going to be read.");

        if (length <= 0)
            throw new IOException("The length to be read cannot 0 or smaller.");

        int read = 0;

        while (read < length) {
            int len = inputStream.read(buffer, read, length - read);

            if (len == -1)
                throw new IOException("Target closed connection before reading all the data.");
            else if (len > 0)
                read += len;
        }

        return buffer;
    }

    private void read(InputStream inputStream, OutputStream outputStream, byte[] buffer, long length)
            throws IOException, TimeoutException
    {
        long read = 0;

        while (read < length) {
            int len = (int) Math.min(buffer.length, length - read);
            read(inputStream, buffer, len);
            read += len;
            outputStream.write(buffer, 0, len);
            outputStream.flush();
        }
    }

    /**
     * Receive from the remote. Unlike {@link ActiveConnection#receive(OutputStream)} this expects the
     * {@link Response} to have the {@link Response#index} field to be set.
     *
     * @return the response received from the remote.
     * @throws IOException      when something goes during read or similar.
     * @throws TimeoutException when connection times out during the read process or similar.
     */
    public Response receive() throws IOException, TimeoutException
    {
        return receive(new ByteArrayOutputStream());
    }

    /**
     * When the opposite side calls {@link ActiveConnection#reply(String)}, this method should
     * be invoked so that the communication occurs. The order of the calls should be in order
     * with their peers. For instance, when you call {@link ActiveConnection#reply(String)} method,
     * the other side should already have called this method or vice versa, which means asynchronous
     * task should not block the thread when the data is being transferred.
     *
     * @param outputStream where the index will be written. It is made available as {@link Response#index}
     *                     when it is a {@link ByteArrayOutputStream} instance.
     * @return The response that is received.
     * @throws IOException      When a socket IO error occurs.
     * @throws TimeoutException When the amount time exceeded while waiting for another byte to
     *                          transfer.
     * @throws JSONException    When the JSON parsing error occurs.
     * @see #reply(String)
     * @see #reply(String, JSONObject)
     */
    public synchronized Response receive(OutputStream outputStream) throws IOException, TimeoutException, JSONException
    {
        InputStream inputStream = getSocket().getInputStream();
        ByteArrayOutputStream header = new ByteArrayOutputStream();
        final byte[] buffer = new byte[8096];

        int flags = readAsByteBuffer(inputStream, buffer, 0, Integer.BYTES).getInt();
        short headerLength = readAsByteBuffer(inputStream, buffer, 0, Short.BYTES).getShort();

        if (headerLength > 0)
            read(inputStream, header, buffer, headerLength);

        int indexLength = readAsByteBuffer(inputStream, buffer, 0, Integer.BYTES).getInt();

        if (indexLength > 0)
            read(inputStream, outputStream, buffer, indexLength);

        return new Response(getSocket().getRemoteSocketAddress(), flags, headerLength, indexLength, header,
                outputStream instanceof ByteArrayOutputStream ? (ByteArrayOutputStream) outputStream : null);
    }

    /**
     * This will send reply to the other side with no extra header data.
     *
     * @see #reply(String, JSONObject)
     */
    public void reply(String out) throws IOException, JSONException
    {
        reply(out, new JSONObject());
    }

    /**
     * A complimentary call to {@link ActiveConnection#reply(byte[], JSONObject)} to send a string transforming
     * it into bytes before sending.
     *
     * @param out    the string to send.
     * @param header the header that will be sent to the client with the length of the data and other data depending
     *               on your needs.
     * @throws IOException   When a socket IO error occurs.
     * @throws JSONException When the JSON parsing error occurs.
     * @see #reply(byte[], JSONObject)
     */
    public void reply(String out, JSONObject header) throws IOException, JSONException
    {
        reply(out.getBytes(), header);
    }

    /**
     * A complimentary call to {@link ActiveConnection#reply(byte[], int, int, int, JSONObject)} to send a byte array
     * with fixed length.
     *
     * @param out    the data to send.
     * @param header the header that will be sent to the client with the length of the data and other data depending
     *               on your needs.
     * @throws IOException   When a socket IO error occurs.
     * @throws JSONException When the JSON parsing error occurs.
     */
    public void reply(byte[] out, JSONObject header) throws IOException, JSONException
    {
        reply(out, 0, out.length, FLAG_NONE, header);
    }

    /**
     * This will send the given data to the other side while the other side has already called
     * {@link ActiveConnection#receive()}.
     *
     * @param out    the data to send.
     * @param offset where we will begin reading the data.
     * @param length where we will stop reading the data.
     * @param flags  that specify the features used for this packet.
     * @param header the header that will be sent to the client with the length of the data and other data depending
     *               on your needs.
     * @throws IOException   When a socket IO error occurs.
     * @throws JSONException When the JSON parsing error occurs.
     * @see ActiveConnection#receive()
     */
    public synchronized void reply(byte[] out, int offset, int length, int flags, JSONObject header)
            throws IOException, JSONException
    {
        if (offset < 0 || (out.length > 0 && offset >= out.length))
            throw new ArrayIndexOutOfBoundsException("The offset cannot be smaller than 0, or equal to or larger " +
                    "than the actual size of the data.");

        if (length < 0 || (out.length > 0 && length > out.length))
            throw new ArrayIndexOutOfBoundsException("The length cannot be 0 or larger than the actual " +
                    "size of the data.");

        OutputStream outputStream = getSocket().getOutputStream();
        int size = length - offset;

        reply(outputStream, flags, header.toString().getBytes());

        outputStream.write(ByteBuffer.allocate(Integer.BYTES).putInt(size).array());
        outputStream.flush();

        outputStream.write(out, offset, length);
        outputStream.flush();
    }

    private void reply(OutputStream outputStream, int flags, byte[] header) throws IOException
    {
        // The first 16 bit data represents length of the header
        if (header.length > Short.MAX_VALUE)
            throw new IllegalStateException("The maximum length of a header can be " + (Short.MAX_VALUE));

        // put length of the header
        outputStream.write(ByteBuffer.allocate(Integer.BYTES).putInt(flags).array());
        outputStream.write(ByteBuffer.allocate(Short.BYTES).putShort((short) header.length).array());
        outputStream.write(header);
    }
}