package com.genonbeta.coolsocket;

import org.json.JSONException;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;

/**
 * This class connects to both clients and servers. This accepts a valid socket instance, and writes to and reads from
 * it.
 */
public class ActiveConnection implements Closeable
{
    public static final String TAG = ActiveConnection.class.getSimpleName();

    private final Socket socket;

    private OutputStream privOutputStream;

    private InputStream privInputStream;

    private int internalCacheLimit = 256 * 1024;

    private int flags;

    private long length;

    private long totalLength;

    /**
     * An instance with socket connection to a CoolSocket server.
     *
     * @param socket The connection to CoolSocket server or client.
     */
    public ActiveConnection(Socket socket)
    {
        if (socket == null)
            throw new NullPointerException("Socket cannot be null.");

        if (!socket.isConnected())
            throw new IllegalStateException("Socket should have a valid connection.");

        this.socket = socket;
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
        if (socket != null)
            socket.close();
    }

    /**
     * Connects to a CoolSocket server.
     *
     * @param socketAddress the server address to connection.
     * @param readTimeout   the maximum time allowed during reading from the input channel.
     * @return the connection object representing an active connection.
     * @throws IOException if connection fails for some reason.
     */
    public static ActiveConnection connect(SocketAddress socketAddress, int readTimeout) throws IOException
    {
        Socket socket = new Socket();
        socket.bind(null);
        socket.connect(socketAddress);

        return new ActiveConnection(socket, readTimeout);
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
     * @return the address that the socket is bound to.
     */
    public InetAddress getAddress()
    {
        return getSocket().getInetAddress();
    }

    /**
     * @return the limit for internal caching when reading into the RAM.
     */
    public int getInternalCacheLimit()
    {
        return internalCacheLimit;
    }

    public InputStream getPrivInputStream() throws IOException
    {
        if (privInputStream == null)
            privInputStream = getSocket().getInputStream();
        return privInputStream;
    }

    public OutputStream getPrivOutputStream() throws IOException
    {
        if (privOutputStream == null)
            privOutputStream = getSocket().getOutputStream();
        return privOutputStream;
    }


    /**
     * The socket that is used to communicate
     *
     * @return Null if no socket was provided or the socket instance.
     */
    public Socket getSocket()
    {
        return socket;
    }

    private byte[] read(InputStream inputStream, byte[] buffer, int length) throws IOException
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

    private void read(InputStream inputStream, OutputStream outputStream, byte[] buffer, long length) throws IOException
    {
        long read = 0;

        while (read < length) {
            int len = (int) Math.min(buffer.length, length - read);
            read(inputStream, buffer, len);
            read += len;
            outputStream.write(buffer, 0, len);
        }

        outputStream.flush();
    }

    private ByteBuffer readAsByteBuffer(InputStream inputStream, byte[] buffer, int offset, int length)
            throws IOException
    {
        return ByteBuffer.wrap(read(inputStream, buffer, length), offset, length);
    }

    public Response receive() throws IOException
    {
        return receive(new ByteArrayOutputStream());
    }

    public Response receive(OutputStream outputStream) throws IOException
    {
        return receive(outputStream, -1);
    }

    /**
     * Receive from the remote into the given stream.
     *
     * @param outputStream to write into.
     * @param maxLength    that can be read into the output stream. '-1' will mean no limit.
     * @return The response that is received.
     * @throws IOException   when a socket IO error occurs, or when the max length for the data readable is exceeded.
     * @throws JSONException when the JSON parsing error occurs.
     * @see #reply(String)
     */
    public synchronized Response receive(OutputStream outputStream, int maxLength) throws IOException
    {
        final byte[] buffer = new byte[8096];

        int flags = readAsByteBuffer(getPrivInputStream(), buffer, 0, Integer.BYTES).getInt();
        boolean chunked = (flags & CoolSocket.FLAG_DATA_CHUNKED) != 0;
        long totalLength = 0;
        long length;

        do {
            length = readAsByteBuffer(getPrivInputStream(), buffer, 0, Long.BYTES).getLong();
            totalLength += length;

            if (maxLength > 0 && totalLength > maxLength)
                throw new SizeExceededException("The length of the data exceeds the maximum length.");

            if (length > 0)
                read(getPrivInputStream(), outputStream, buffer, length);
        } while (chunked && length > -1);

        return new Response(getSocket().getRemoteSocketAddress(), flags, length,
                outputStream instanceof ByteArrayOutputStream ? new ByteArrayOutputStream() : null);
    }

    public void reply(String out) throws IOException
    {
        writeBegin(false, 0);
        writePart(out.getBytes());
        writeEnd();
    }

    public void replyWithFixedLength(InputStream inputStream, long fixedSize) throws IOException
    {
        writeBegin(false, 0);
        writeFrom(inputStream, fixedSize);
        writeEnd();
    }

    public void replyWithChunked(InputStream inputStream) throws IOException
    {
        writeBegin(true, 0);
        writeFrom(inputStream);
        writeEnd();
    }

    /**
     * Set the limit for maximum read from remote when the output stream self provided. This will not be used for
     * custom output streams.
     *
     * @param internalCacheLimit the limit in bytes.
     */
    public void setInternalCacheLimit(int internalCacheLimit)
    {
        this.internalCacheLimit = internalCacheLimit;
    }

    /**
     * Prepare for sending a part. This will send the flags for the upcoming part transmission so that the remote will
     * know how to treat the data it receives. After this method call, you should call
     * {@link #writePart(byte[], int, int)} to begin writing bytes or end the part with the {@link #writeEnd()}
     * method call.
     *
     * @param chunked this means that the length put through {@link #writePart(byte[], int, int)} is incomplete and the
     *                full length of the data is unknown. It should keep receiving until it gets -1 value from the
     *                length data.
     * @param flags   the feature flags set for this part. It sets how the remote should handle the data it
     * @throws IOException when socket related IO error occurs.
     */
    public synchronized void writeBegin(boolean chunked, int flags) throws IOException
    {
        if (chunked)
            flags |= CoolSocket.FLAG_DATA_CHUNKED;

        getPrivOutputStream().write(ByteBuffer.allocate(Integer.BYTES).putInt(flags).array());
        this.flags = flags;
    }

    private void writePrivSize(long size) throws IOException
    {
        getPrivOutputStream().write(ByteBuffer.allocate(Long.BYTES).putLong(size).array());
    }

    public synchronized void writePart(byte[] bytes) throws IOException
    {
        writePart(bytes, 0, bytes.length);
    }

    public synchronized void writePart(byte[] bytes, int offset, int length) throws IOException
    {
        if (offset < 0 || (bytes.length > 0 && offset >= bytes.length))
            throw new ArrayIndexOutOfBoundsException("The offset cannot be smaller than 0, or equal to or larger " +
                    "than the actual size of the data.");

        if (length < 0 || (bytes.length > 0 && length > bytes.length))
            throw new ArrayIndexOutOfBoundsException("The length cannot be 0 or larger than the actual size of the " +
                    "data.");

        writePrivSize(length - offset);
        getPrivOutputStream().write(bytes, offset, length);
    }

    public synchronized void writeFrom(InputStream inputStream) throws IOException
    {
        writeFrom(inputStream, -1);
    }

    public synchronized void writeFrom(InputStream inputStream, long totalLength) throws IOException
    {
        byte[] buffer = new byte[8096];
        long readLength = 0;
        int length;

        if (totalLength >= 0)
            writePrivSize(totalLength);

        if (totalLength != 0)
            while ((totalLength < 0 || readLength < totalLength) && (length = inputStream.read(buffer)) != -1) {
                if (length > 0) {
                    readLength += length;

                    if (totalLength == -1)
                        writePrivSize(length);
                    else if (totalLength > 0 && readLength > totalLength)
                        length -= readLength - totalLength;

                    getPrivOutputStream().write(buffer, 0, length);
                }
            }
    }

    public synchronized void writeEnd() throws IOException
    {
        if ((flags & CoolSocket.FLAG_DATA_CHUNKED) != 0)
            writePrivSize(-1);

        getPrivOutputStream().flush();
    }
}