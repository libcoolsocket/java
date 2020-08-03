package com.genonbeta.coolsocket;

import com.genonbeta.coolsocket.response.Flags;
import com.genonbeta.coolsocket.response.Response;
import com.genonbeta.coolsocket.response.SizeExceededException;
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

    private InputStream getInputStreamPriv() throws IOException
    {
        if (privInputStream == null)
            privInputStream = getSocket().getInputStream();
        return privInputStream;
    }

    private OutputStream getOutputStreamPriv() throws IOException
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

    public int read(Description description, byte[] buffer) throws IOException
    {
        boolean chunked = description.flags.chunked();

        if (chunked && description.awaitingChunkSize <= 0) {
            description.awaitingChunkSize = readSize(buffer);

            if (description.awaitingChunkSize == CoolSocket.LENGTH_UNSPECIFIED)
                return CoolSocket.LENGTH_UNSPECIFIED;
        }

        int readAsMuch = (int) (chunked
                ? (description.awaitingChunkSize < buffer.length ? description.awaitingChunkSize : buffer.length)
                : (description.leftLength() < buffer.length ? description.leftLength() : buffer.length));
        int len = getInputStreamPriv().read(buffer, 0, readAsMuch);
        description.handedLength += len;

        if (chunked) {
            description.awaitingChunkSize -= len;
            description.totalLength += len;
        }

        return len;
    }

    public Description readBegin(byte[] buffer) throws IOException
    {
        Flags flags = new Flags(readFlags(buffer));
        return new Description(flags, flags.chunked() ? CoolSocket.LENGTH_UNSPECIFIED : readSize(buffer));
    }

    protected long readFlags(byte[] buffer) throws IOException
    {
        return readAsMuch(buffer, 0, Long.BYTES).getLong();
    }

    protected long readSize(byte[] buffer) throws IOException
    {
        return readAsMuch(buffer, 0, Long.BYTES).getLong();
    }

    protected ByteBuffer readAsMuch(byte[] buffer, int offset, int length) throws IOException
    {
        if (length < 1 || length > buffer.length)
            throw new IndexOutOfBoundsException("length cannot be a negative value, or be larger than the buffer.");

        int read = 0;
        int len;

        while ((len = getInputStreamPriv().read(buffer, read, length - read)) != -1 && read < length)
            read += len;

        if (read < length)
            throw new IOException("Target closed connection before reading as many data.");

        return ByteBuffer.wrap(buffer, offset, length);
    }

    public Response receive() throws IOException
    {
        return receive(new ByteArrayOutputStream(), getInternalCacheLimit());
    }

    public Response receive(OutputStream outputStream) throws IOException
    {
        return receive(outputStream, CoolSocket.LENGTH_UNSPECIFIED);
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
        int len;

        Description description = readBegin(buffer);
        boolean chunked = description.flags.chunked();

        do {
            len = read(description, buffer);

            if (maxLength > 0 && description.handedLength > maxLength)
                throw new SizeExceededException("The length of the data exceeds the maximum length.");

            if (len > 0)
                outputStream.write(buffer, 0, len);
        } while (!description.done());

        return new Response(getSocket().getRemoteSocketAddress(), description.flags.all(), description.totalLength,
                outputStream instanceof ByteArrayOutputStream ? (ByteArrayOutputStream) outputStream : null);
    }

    public void reply(String out) throws IOException
    {
        byte[] bytes = out.getBytes();
        Description description = writeBegin(0, bytes.length);
        write(description, bytes);
        writeEnd(description);
    }

    public void replyWithFixedLength(long flags, InputStream inputStream, long fixedSize) throws IOException
    {
        Description description = writeBegin(flags, fixedSize);
        writeAll(description, inputStream);
        writeEnd(description);
    }

    public void replyWithChunked(long flags, InputStream inputStream) throws IOException
    {
        Description description = writeBegin(0, CoolSocket.LENGTH_UNSPECIFIED);
        writeAll(description, inputStream);
        writeEnd(description);
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
     * {@link #write(Description, byte[], int, int)} to begin writing bytes or end the part with the
     * {@link #writeEnd(Description)} method call.
     *
     * @param flags       the feature flags set for this part. It sets how the remote should handle the data it
     * @param totalLength the total length of the data that will be sent. Use {@link CoolSocket#LENGTH_UNSPECIFIED} when
     *                    the length is unknown at the moment. Doing so will make this transmission process
     *                    {@link Flags#FLAG_DATA_CHUNKED} where the data length will only be visible as much as
     *                    we read from the source.
     * @throws IOException when socket related IO error occurs.
     */
    public synchronized Description writeBegin(long flags, long totalLength) throws IOException
    {
        if (totalLength == CoolSocket.LENGTH_UNSPECIFIED)
            flags |= Flags.FLAG_DATA_CHUNKED;

        writeFlags(flags);

        if (totalLength > CoolSocket.LENGTH_UNSPECIFIED)
            writeSize(totalLength);

        return new Description(flags, totalLength);
    }

    protected void writeFlags(long flags) throws IOException
    {
        getOutputStreamPriv().write(ByteBuffer.allocate(Long.BYTES).putLong(flags).array());
    }

    protected void writeSize(long size) throws IOException
    {
        getOutputStreamPriv().write(ByteBuffer.allocate(Long.BYTES).putLong(size).array());
    }

    public synchronized void write(Description description, byte[] bytes) throws IOException
    {
        write(description, bytes, 0, bytes.length);
    }

    public synchronized void write(Description description, byte[] bytes, int offset, int length)
            throws IOException
    {
        if (offset < 0 || (bytes.length > 0 && offset >= bytes.length))
            throw new ArrayIndexOutOfBoundsException("The offset cannot be smaller than 0, or equal to or larger " +
                    "than the actual size of the data.");

        if (length < 0 || (bytes.length > 0 && length > bytes.length))
            throw new ArrayIndexOutOfBoundsException("The length cannot be 0 or larger than the actual size of the " +
                    "data.");

        int size = length - offset;
        description.handedLength += size;

        if (description.flags.chunked()) {
            writeSize(size);
            description.totalLength += size;
        } else if (description.handedLength > description.totalLength)
            throw new SizeExceededException("The size of the data exceeds that length notified to the remote.");

        getOutputStreamPriv().write(bytes, offset, length);
    }

    public synchronized void writeAll(Description description, InputStream inputStream) throws IOException
    {
        byte[] buffer = new byte[8096];
        int len;

        while ((len = inputStream.read(buffer)) != -1 && !description.done()) {
            write(description, buffer, 0, len);
        }
    }

    public synchronized void writeEnd(Description description) throws IOException
    {
        if (description.flags.chunked())
            writeSize(-1);

        getOutputStreamPriv().flush();
    }

    public static class Description
    {
        public final Flags flags;

        public long handedLength;

        public long totalLength;

        public long awaitingChunkSize;

        public Description(long flags, long totalLength)
        {
            this(new Flags(flags), totalLength);
        }

        public Description(Flags flags, long totalLength)
        {
            if (flags == null)
                throw new NullPointerException("Flags cannot be null.");

            this.flags = flags;
            this.totalLength = totalLength == CoolSocket.LENGTH_UNSPECIFIED ? 0 : totalLength;
        }

        public boolean done()
        {
            return (flags.chunked() && awaitingChunkSize == CoolSocket.LENGTH_UNSPECIFIED)
                    || (!flags.chunked() && leftLength() == 0);
        }

        public long leftLength()
        {
            return totalLength - handedLength;
        }
    }
}