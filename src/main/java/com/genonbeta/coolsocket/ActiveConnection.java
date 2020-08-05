package com.genonbeta.coolsocket;

import com.genonbeta.coolsocket.config.Config;
import com.genonbeta.coolsocket.response.*;
import org.json.JSONException;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

/**
 * This class connects to both clients and servers. This accepts a valid socket instance, and writes to and reads from
 * it.
 */
public class ActiveConnection implements Closeable
{
    private final Socket socket;

    private OutputStream privOutputStream;

    private InputStream privInputStream;

    private int internalCacheLimit = 256 * 1024;

    private int protocolVersion;

    private boolean cancelled;

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
     * @throws SocketException if setting the socket timeout fails.
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
     * Cancel the upcoming or the ongoing read and write operation by throwing an error. The cancelled state will
     * only be cleared after {@link CancelledException} is thrown or {@link #cancelled()} is invoked.
     */
    public void cancel()
    {
        cancelled = true;
    }

    /**
     * Check whether there is a cancellation request. Calling this will clear the request as you are expected to handle
     * it. If you are not going to but something else is, then invoke the {@link #cancel()} method after calling this
     * so that it can see the request.
     *
     * @return true if there is a cancellation request.
     */
    public boolean cancelled()
    {
        if (cancelled) {
            cancelled = false;
            return true;
        }
        return false;
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

    private InfoExchange exchangeReceive() throws IOException
    {
        byte[] buffer = new byte[8];
        int featureId = readInteger(buffer);
        InfoExchange infoExchange = InfoExchange.values()[featureId];
        int maxLength = infoExchange.maxLength;
        int firstInteger = readInteger(buffer);
        if (firstInteger > maxLength)
            throw new SizeExceededException("The remote reported size for " + infoExchange + " info cannot be bigger " +
                    "than " + maxLength + ". The remote reported it as " + firstInteger);

        switch (infoExchange) {
            case Dummy:
                readExactIntoBuffer(new byte[firstInteger], firstInteger);
                break;
            case ProtocolVersion:
                protocolVersion = firstInteger;
        }

        return infoExchange;
    }

    private void exchangeSend(InfoExchange infoExchange) throws IOException
    {
        writeInteger(infoExchange.ordinal());

        int firstInteger;
        byte[] bytes;
        switch (infoExchange) {
            case ProtocolVersion:
                firstInteger = Config.PROTOCOL_VERSION;
                bytes = null;
                break;
            case Dummy:
            default:
                bytes = "Dummy, dum dum!".getBytes();
                firstInteger = bytes.length;
        }

        writeInteger(firstInteger);
        if (bytes != null)
            getOutputStreamPriv().write(bytes);
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
            System.out.println("Connections should be closed before their references are being destroyed");
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
     * Get the protocol version of this implementation.
     *
     * @return the protocol version.
     */
    public static int getProtocolVersion()
    {
        return Config.PROTOCOL_VERSION;
    }

    /**
     * Returns the remote protocol version that becomes available after the first interaction.
     *
     * @return the remote protocol version.
     */
    public int getProtocolVersionOfRemote()
    {
        return protocolVersion;
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

    public void handleByteBreak(Description description, boolean localSending) throws IOException
    {
        if (cancelled())
            description.byteBreakLocal = ByteBreak.Cancel;
        else if (protocolVersion == 0) {
            description.byteBreakLocal = ByteBreak.InfoExchange;
            description.pendingExchange = InfoExchange.ProtocolVersion;
        } else
            description.byteBreakLocal = ByteBreak.None;

        if (localSending) {
            writeByteBreak(description.byteBreakLocal);
            description.byteBreakRemote = readByteBreak();
        } else {
            description.byteBreakRemote = readByteBreak();
            writeByteBreak(description.byteBreakLocal);
        }

        if (description.byteBreakLocal.equals(ByteBreak.Cancel) || description.byteBreakRemote.equals(ByteBreak.Cancel))
            throw new CancelledException("This operation has been cancelled.", description.byteBreakRemote.equals(
                    ByteBreak.Cancel));

        // If one side sends None, it will mean unsupported. If both sides send None, it will mean, no operation
        // requested by both sides.
        if (!description.byteBreakLocal.equals(description.byteBreakRemote)
                || description.byteBreakLocal.equals(ByteBreak.None))
            return;

        switch (description.byteBreakLocal) {
            case InfoExchange:
                if (localSending)
                    exchangeSend(exchangeReceive());
                else {
                    exchangeSend(description.pendingExchange);
                    exchangeReceive();
                }
                break;
            default:
                return;
        }

        handleByteBreak(description, localSending);
    }

    public int read(Description description) throws IOException
    {
        byte[] buffer = description.buffer;
        boolean chunked = description.flags.chunked();
        long available = 0;

        if (description.awaitingSize <= 0) {
            handleByteBreak(description, false);
            description.awaitingSize = readSize(buffer);
            available = description.available();

            if (available == CoolSocket.LENGTH_UNSPECIFIED)
                return CoolSocket.LENGTH_UNSPECIFIED;
        }

        int len = getInputStreamPriv().read(buffer, 0, (int) Math.min(description.buffer.length, available));
        description.handedLength += len;
        description.awaitingSize -= len;

        if (chunked)
            description.totalLength += len;

        return len;
    }

    public Description readBegin() throws IOException
    {
        return readBegin(new byte[8096]);
    }

    public Description readBegin(byte[] buffer) throws IOException
    {
        Flags flags = new Flags(readFlags(buffer));
        Description description = new Description(flags, flags.chunked() ? CoolSocket.LENGTH_UNSPECIFIED
                : readSize(buffer), buffer);

        handleByteBreak(description, false);

        return description;
    }

    private int readByte() throws IOException
    {
        return getInputStreamPriv().read();
    }

    protected ByteBreak readByteBreak() throws IOException
    {
        return ByteBreak.from(readByte());
    }

    protected void readExact(byte[] buffer, int length) throws IOException
    {
        if (length < 1 || length > buffer.length)
            throw new IndexOutOfBoundsException("length cannot be a negative value, or be larger than the buffer.");

        int read = 0;
        int len;

        while ((len = getInputStreamPriv().read(buffer, read, length - read)) != -1 && read < length)
            read += len;

        if (read < length)
            throw new IOException("Target closed connection before reading as many data.");
    }

    protected ByteBuffer readExactIntoBuffer(byte[] buffer, int length) throws IOException
    {
        readExact(buffer, length);
        return ByteBuffer.wrap(buffer, 0, length);
    }

    protected long readFlags(byte[] buffer) throws IOException
    {
        return readLong(buffer);
    }

    protected int readInteger(byte[] buffer) throws IOException
    {
        return readExactIntoBuffer(buffer, Integer.BYTES).getInt();
    }

    protected long readLong(byte[] buffer) throws IOException
    {
        return readExactIntoBuffer(buffer, Long.BYTES).getLong();
    }

    protected long readSize(byte[] buffer) throws IOException
    {
        return readLong(buffer);
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
        int len;
        Description description = readBegin();

        do {
            len = read(description);

            if (maxLength > 0 && description.handedLength > maxLength)
                throw new SizeExceededException("The length of the data exceeds the maximum length.");

            if (len > 0)
                outputStream.write(description.buffer, 0, len);
        } while (description.hasAvailable());

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

    public void replyInChunks(long flags, InputStream inputStream) throws IOException
    {
        Description description = writeBegin(flags, CoolSocket.LENGTH_UNSPECIFIED);
        writeAll(description, inputStream);
        writeEnd(description);
    }

    public void replyWithFixedLength(long flags, InputStream inputStream, long fixedSize) throws IOException
    {
        Description description = writeBegin(flags, fixedSize);
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

    public void write(Description description, byte[] bytes) throws IOException
    {
        write(description, bytes, 0, bytes.length);
    }

    public void write(Description description, int offset, int length) throws IOException
    {
        write(description, description.buffer, offset, length);
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

        if (description.flags.chunked())
            description.totalLength += size;
        else if (description.handedLength > description.totalLength)
            throw new SizeExceededException("The size of the data exceeds that length notified to the remote.");

        handleByteBreak(description, true);
        writeSize(size);

        getOutputStreamPriv().write(bytes, offset, length);
    }

    public synchronized void writeAll(Description description, InputStream inputStream) throws IOException
    {
        int len;
        while ((len = inputStream.read(description.buffer)) != -1) {
            write(description, 0, len);
        }
    }

    /**
     * Prepare for sending a response. This will send the flags for the upcoming data transmission so that the remote
     * will know how to treat the data it receives. After this method call, you should invoke
     * {@link #write(Description, byte[], int, int)} to begin writing bytes or end the part with the
     * {@link #writeEnd(Description)} method call.
     *
     * @param flags       the feature flags set for this part. It sets how the remote should handle the data it
     * @param totalLength the total length of the data that will be sent. Use {@link CoolSocket#LENGTH_UNSPECIFIED} when
     *                    the length is unknown at the moment. Doing so will make this transmission process
     *                    {@link Flags#FLAG_DATA_CHUNKED} where the data length will only be visible as much as
     *                    we read from the source.
     * @return the description object for this write operation.
     * @throws IOException when socket related IO error occurs.
     */
    public synchronized Description writeBegin(long flags, long totalLength) throws IOException
    {
        byte[] buffer = new byte[8096];

        if (totalLength == CoolSocket.LENGTH_UNSPECIFIED)
            flags |= Flags.FLAG_DATA_CHUNKED;

        writeFlags(flags);

        Description description = new Description(flags, totalLength, buffer);

        if (totalLength > CoolSocket.LENGTH_UNSPECIFIED)
            writeSize(totalLength);

        handleByteBreak(description, true);

        return description;
    }

    protected void writeByte(int bite) throws IOException
    {
        getOutputStreamPriv().write(bite);
    }

    protected void writeByteBreak(ByteBreak byteBreak) throws IOException
    {
        writeByte(byteBreak.ordinal());
    }

    public synchronized void writeEnd(Description description) throws IOException
    {
        if (description.flags.chunked()) {
            handleByteBreak(description, true);
            writeSize(-1);
        }

        getOutputStreamPriv().flush();
    }

    protected void writeFlags(long flags) throws IOException
    {
        writeSize(flags);
    }

    protected void writeInteger(int value) throws IOException
    {
        getOutputStreamPriv().write(ByteBuffer.allocate(Integer.BYTES).putInt(value).array());
    }

    protected void writeLong(long value) throws IOException
    {
        getOutputStreamPriv().write(ByteBuffer.allocate(Long.BYTES).putLong(value).array());
    }

    protected void writeSize(long size) throws IOException
    {
        writeLong(size);
    }

    public static class Description
    {
        public final Flags flags;

        public long handedLength;

        public long totalLength;

        public long awaitingSize;

        public final byte[] buffer;

        public ByteBreak byteBreakLocal = null;

        public ByteBreak byteBreakRemote = null;

        public InfoExchange pendingExchange = null;

        public Description(long flags, long totalLength, byte[] buffer)
        {
            this(new Flags(flags), totalLength, buffer);
        }

        public Description(Flags flags, long totalLength, byte[] buffer)
        {
            if (flags == null)
                throw new NullPointerException("Flags cannot be null.");

            if (buffer == null)
                throw new NullPointerException("Buffer cannot be null.");

            if (buffer.length < 8096)
                throw new BufferUnderflowException();

            this.flags = flags;
            this.totalLength = totalLength <= CoolSocket.LENGTH_UNSPECIFIED ? 0 : totalLength;
            this.buffer = new byte[8096];
        }

        public long available()
        {
            return flags.chunked() ? awaitingSize
                    : (totalLength <= handedLength ? CoolSocket.LENGTH_UNSPECIFIED : totalLength - handedLength);
        }

        public boolean hasAvailable()
        {
            return available() != CoolSocket.LENGTH_UNSPECIFIED;
        }
    }
}