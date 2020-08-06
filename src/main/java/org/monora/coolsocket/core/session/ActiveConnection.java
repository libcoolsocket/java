package org.monora.coolsocket.core.session;

import org.json.JSONObject;
import org.monora.coolsocket.core.CoolSocket;
import org.monora.coolsocket.core.config.Config;
import org.monora.coolsocket.core.response.*;

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

    private boolean closeRequested;

    /**
     * Create an instance with a socket connection to a CoolSocket server.
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
     * @throws SocketException If setting the socket timeout fails.
     */
    public ActiveConnection(Socket socket, int timeout) throws SocketException
    {
        this(socket);
        socket.setSoTimeout(timeout);
    }

    /**
     * Close the socket, and thus this connection instance.
     * <p>
     * This will not inform the remote. If you want the connection to be closed when both sides are ready, then, use
     * {@link #closeSafely()}.
     *
     * @throws IOException If IO error occurs, or the socket is already closed.
     * @see #closeSafely()
     */
    @Override
    public void close() throws IOException
    {
        socket.close();
    }

    /**
     * Check whether there is a request on closing the connection.
     * <p>
     * Once requested, this will lead the closing of the connection. This should not be handled manually. The usual
     * workflow should be carried out so that the remote will also be ready when the connection is finally closed.
     *
     * @return True if there is a request to close the connection. Which side requested it can be checked using the
     * {@link ClosedException#remoteRequested} field.
     */
    public boolean closeRequested()
    {
        return closeRequested;
    }

    /**
     * Close the connection with mutual agreement.
     * <p>
     * The connection will be closed when both sides is ready, in other words, the remote will know that this was
     * requested by you.
     * <p>
     * If you want to close the connection immediately, use {@link #close()}.
     *
     * @throws IOException If the socket is already closed.
     * @see #close()
     */
    public void closeSafely() throws IOException
    {
        if (getSocket().isClosed())
            throw new IOException("Socket is already closed.");
        closeRequested = true;
    }

    /**
     * Cancel the upcoming or the ongoing read and write operation by throwing an error.
     * <p>
     * The cancelled state will be cleared after {@link CancelledException} is thrown or {@link #cancelled()} is
     * invoked.
     *
     * @throws IOException If the socket is closed.
     */
    public void cancel() throws IOException
    {
        if (getSocket().isClosed())
            throw new IOException("Socket is closed.");
        cancelled = true;
    }

    /**
     * Check whether there is a cancellation request.
     * <p>
     * Calling this will clear the request as you are expected to handle it. If you are not going to but something else
     * is, then invoke the {@link #cancel()} method after calling this so that it can see the request.
     *
     * @return True if there is a cancellation request.
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
     * <p>
     * The read timeout defaults to 0.
     *
     * @param socketAddress The server address to connection.
     * @return The connection object representing an active connection.
     * @throws IOException If connection fails for some reason.
     */
    public static ActiveConnection connect(SocketAddress socketAddress) throws IOException
    {
        return connect(socketAddress, 0);
    }

    /**
     * Connects to a CoolSocket server.
     *
     * @param socketAddress The server address to connection.
     * @param readTimeout   The maximum time allowed during reading from the input channel.
     * @return The connection object representing an active connection.
     * @throws IOException If connection fails for some reason.
     */
    public static ActiveConnection connect(SocketAddress socketAddress, int readTimeout) throws IOException
    {
        Socket socket = new Socket();
        socket.bind(null);
        socket.connect(socketAddress);

        return new ActiveConnection(socket, readTimeout);
    }

    // TODO: 8/6/20 Improve this

    /**
     * Retrieve the {@link InfoExchange} value from the remote and do the appropriate operation based on that.
     * <p>
     * For instance, the remote may send us the protocol version using the {@link InfoExchange#ProtocolVersion} value.
     * After that we read the next integer that we use as the value or the length depending on the request type.
     *
     * @return The value that was executed.
     * @throws IOException If an IO error occurs.
     */
    private InfoExchange exchangeReceive() throws IOException
    {
        byte[] buffer = new byte[8];
        int featureId = readInteger(buffer);
        InfoExchange infoExchange = InfoExchange.from(featureId);
        int maxLength = infoExchange.maxLength;
        int length = readInteger(buffer);
        if (length > maxLength)
            throw new SizeExceededException("The remote reported size for " + infoExchange + "is too large.",
                    maxLength, length);

        switch (infoExchange) {
            case Dummy:
                readExactIntoBuffer(new byte[length], length);
                break;
            case ProtocolVersion:
                protocolVersion = length;
        }

        return infoExchange;
    }

    /**
     * Send the info that was requested by us or by the remote.
     *
     * @param infoExchange The type info that is being exchanged.
     * @throws IOException If an IO error occurs.
     */
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
     * This ensures the connection is closed before this instance of the class is destroyed.
     *
     * @throws Throwable Override to use this feature.
     * @deprecated By the parent
     */
    @Override
    @Deprecated
    protected void finalize() throws Throwable
    {
        super.finalize();

        if (!getSocket().isClosed()) {
            System.out.println("Connections should be closed before their references are being destroyed");
            getSocket().close();
        }
    }

    /**
     * @return The address that the socket is bound to.
     */
    public InetAddress getAddress()
    {
        return getSocket().getInetAddress();
    }

    /**
     * @return The limit for internal caching when reading into the heap.
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
     * @return The protocol version.
     */
    public static int getProtocolVersion()
    {
        return Config.PROTOCOL_VERSION;
    }

    /**
     * Returns the protocol version of the remote. This becomes available after the first interaction.
     *
     * @return The remote protocol version.
     */
    public int getProtocolVersionOfRemote()
    {
        return protocolVersion;
    }

    /**
     * Get the socket instance that represents the remote.
     *
     * @return The socket instance.
     */
    public Socket getSocket()
    {
        return socket;
    }

    public void handleByteBreak(Description description, boolean localSending) throws IOException
    {
        if (closeRequested())
            description.byteBreakLocal = ByteBreak.Close;
        else if (cancelled())
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

        if (description.byteBreakLocal.equals(ByteBreak.Close) || description.byteBreakRemote.equals(ByteBreak.Close)) {
            try {
                close();
            } catch (Exception ignored) {
            }
            throw new ClosedException("The connection just closed as one of the sides wanted it.",
                    description.byteBreakRemote.equals(ByteBreak.Close));
        } else if (description.byteBreakLocal.equals(ByteBreak.Cancel)
                || description.byteBreakRemote.equals(ByteBreak.Cancel))
            throw new CancelledException("This operation has been cancelled.", description.byteBreakRemote.equals(
                    ByteBreak.Cancel));

        // If one side sends None, it will mean unsupported. If both sides send None, it will mean, no operation
        // requested by both sides.
        if (!description.byteBreakLocal.equals(description.byteBreakRemote)
                || description.byteBreakLocal.equals(ByteBreak.None))
            return;

        if (description.byteBreakLocal == ByteBreak.InfoExchange) {
            if (localSending)
                exchangeSend(exchangeReceive());
            else {
                exchangeSend(description.pendingExchange);
                exchangeReceive();
            }

            description.pendingExchange = null;
        }

        handleByteBreak(description, localSending);
    }

    /**
     * Read from the remote. This reads into the {@link Description#buffer} and returns the length of the bytes read.
     * <p>
     * This should be called after the {@link #readBegin()}.
     *
     * @param description The object representing the operation.
     * @return The length of the bytes read.
     * @throws IOException If an IO error occurs.
     */
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

    /**
     * Prepare for an operation and retrieve the information about that.
     *
     * @return The object representing the operation.
     * @throws IOException If an IO error occurs, {@link CancelledException} if it was requested by any parties.
     * @see #readBegin(byte[])
     */
    public Description readBegin() throws IOException
    {
        return readBegin(new byte[8096]);
    }

    /**
     * Prepare for an operation and retrieve the information about that.
     * <p>
     * This will receive the information from the remote and apply the needed values to the description object that
     * is produced.
     * <p>
     * After this method, you can use the {@link #read(Description)} method to start reading bytes for this operation.
     * <p>
     * After the operation is finished, this will return -1 to indicate that it is done. This will not produce an
     * error even if you keep reading.
     * <p>
     * You can use one of the {@link #receive} methods to read all the bytes at once if you don't need show progress
     * information.
     *
     * @param buffer The buffer to write into. It shouldn't be too small (e.g., 1-8 bytes). For better compatibility,
     *               use the {@link #readBegin()} that doesn't take this as an argument.
     * @return The object representing the operation.
     * @throws IOException If an IO error occurs, {@link CancelledException} if it was requested by any parties.
     * @see #readBegin(byte[])
     */
    public Description readBegin(byte[] buffer) throws IOException
    {
        Flags flags = new Flags(readFlags(buffer));
        Description description = new Description(flags, flags.chunked() ? CoolSocket.LENGTH_UNSPECIFIED
                : readSize(buffer), buffer);

        handleByteBreak(description, false);

        return description;
    }

    /**
     * @return The read byte.
     * @throws IOException If an IO error occurs.
     */
    protected int readByte() throws IOException
    {
        return getInputStreamPriv().read();
    }

    /**
     * Read the byte break value.
     *
     * @return The read byte break value.
     * @throws IOException If an IO error occurs.
     */
    protected ByteBreak readByteBreak() throws IOException
    {
        return ByteBreak.from(readByte());
    }

    /**
     * Read the exact length of data. This will not return unless it reads the data that is the given length long.
     *
     * @param buffer To read into.
     * @param length The length of the data to read.
     * @throws IOException If an IO error occurs.
     * @see #readExactIntoBuffer(byte[], int)
     */
    protected void readExact(byte[] buffer, int length) throws IOException
    {
        if (length < 1 || length > buffer.length)
            throw new IndexOutOfBoundsException("Length cannot be a negative value, or larger than the buffer.");

        int read = 0;
        int len;

        while ((len = getInputStreamPriv().read(buffer, read, length - read)) != -1 && read < length)
            read += len;

        if (read < length)
            throw new SizeFellBehindException("Target closed connection before reading as many data.", length, read);
    }

    /**
     * This will read the exact length of data from the remote and encapsulate it in {@link ByteBuffer} wrapper.
     * Similar to {@link #readExact(byte[], int)}, this will not return unless it reads that much of data.
     *
     * @param buffer To read into.
     * @param length The length of the data to read.
     * @return The byte buffer object that represents the value.
     * @throws IOException If an IO occurs.
     * @see #readExact(byte[], int)
     */
    protected ByteBuffer readExactIntoBuffer(byte[] buffer, int length) throws IOException
    {
        readExact(buffer, length);
        return ByteBuffer.wrap(buffer, 0, length);
    }

    /**
     * Read the flags value which will then be encapsulated by {@link Flags} to analyze the state of the remote.
     *
     * @param buffer To read into.
     * @return The read flags value in the long integer form.
     * @throws IOException If an IO error occurs.
     */
    protected long readFlags(byte[] buffer) throws IOException
    {
        return readLong(buffer);
    }

    /**
     * Read integer from the remote.
     *
     * @param buffer The read into.
     * @return The read integer.
     * @throws IOException If an IO error occurs.
     */
    protected int readInteger(byte[] buffer) throws IOException
    {
        return readExactIntoBuffer(buffer, Integer.BYTES).getInt();
    }

    /**
     * Read long integer from the remote.
     *
     * @param buffer To read into.
     * @return The read long integer.
     * @throws IOException If an IO error occurs.
     */
    protected long readLong(byte[] buffer) throws IOException
    {
        return readExactIntoBuffer(buffer, Long.BYTES).getLong();
    }

    /**
     * Read size from the remote.
     *
     * @param buffer To read into.
     * @return The read size that is in long integer format.
     * @throws IOException If an IO error occurs.
     */
    protected long readSize(byte[] buffer) throws IOException
    {
        return readLong(buffer);
    }

    /**
     * Receive from the remote into the given stream.
     * <p>
     * The output for {@link Response} will be available under the {@link Response#data} field.
     * <p>
     * The output stream defaults to {@link ByteArrayOutputStream}, the length defaults to
     * {@link CoolSocket#LENGTH_UNSPECIFIED}.
     *
     * @return The response that is received.
     * @throws IOException When a socket IO error occurs, when the max length for the data readable is exceeded,
     *                     or {@link CancelledException} if the operation is cancelled.
     * @see #receive()
     * @see #receive(OutputStream)
     */
    public Response receive() throws IOException
    {
        return receive(new ByteArrayOutputStream(), getInternalCacheLimit());
    }

    /**
     * Receive from the remote into the given stream.
     * <p>
     * The length defaults to {@link CoolSocket#LENGTH_UNSPECIFIED} which means the data length is unknown.
     *
     * @param outputStream To write into.
     * @return The response that is received.
     * @throws IOException When a socket IO error occurs, when the max length for the data readable is exceeded,
     *                     or {@link CancelledException} if the operation is cancelled.
     * @see #receive()
     * @see #receive(OutputStream, int)
     */
    public Response receive(OutputStream outputStream) throws IOException
    {
        return receive(outputStream, CoolSocket.LENGTH_UNSPECIFIED);
    }

    /**
     * Receive a response from the remote.
     * <p>
     * This is a complimentary method for other read and write methods. They can also write to this. See the referred
     * methods for other methods that are working together.
     * <p>
     * The data will be available at {@link Response#data} if the given output stream is instance of
     * {@link ByteArrayOutputStream}.
     *
     * @param outputStream To write into.
     * @param maxLength    That can be read into the output stream. '-1' will mean no limit.
     * @return The response object that represents the reply of the remote.
     * @throws IOException If an IO error occurs, or {@link CancelledException} if the operation is cancelled.
     * @see #receive(OutputStream)
     * @see #receive(OutputStream, int)
     * @see #reply(String)
     * @see #reply(long, InputStream)
     * @see #reply(long, InputStream, long)
     * @see #readBegin()
     * @see #read(Description)
     * @see #writeBegin(long, long)
     * @see #write(Description, byte[])
     * @see #write(Description, int, int)
     * @see #write(Description, byte[], int, int)
     * @see #writeEnd(Description)
     * @see #write(Description, InputStream)
     */
    public synchronized Response receive(OutputStream outputStream, int maxLength) throws IOException
    {
        int len;
        Description description = readBegin();

        do {
            len = read(description);

            if (maxLength > 0 && description.handedLength > maxLength)
                throw new SizeExceededException("The length of the data exceeds the maximum length.", maxLength,
                        description.handedLength);

            if (len > 0)
                outputStream.write(description.buffer, 0, len);
        } while (description.hasAvailable());

        return new Response(getSocket().getRemoteSocketAddress(), description.flags.all(), description.totalLength,
                outputStream instanceof ByteArrayOutputStream ? (ByteArrayOutputStream) outputStream : null);
    }

    /**
     * Send the give JSON data to the remote.
     * <p>
     * The given JSON object is first converted to a string and sent via the {@link #reply(String)}.
     *
     * @param jsonObject To send.
     * @throws IOException If an IO error occurs, or {@link CancelledException} if the operation is cancelled.
     */
    public void reply(JSONObject jsonObject) throws IOException
    {
        reply(jsonObject.toString());
    }

    /**
     * Send the given string data to the remote.
     * <p>
     * The bytes are generated from the string.
     *
     * @param string To send.
     * @throws IOException If an IO error occurs, or {@link CancelledException} if the operation is cancelled.
     * @see #reply(long, byte[])
     */
    public void reply(String string) throws IOException
    {
        reply(0, string.getBytes());
    }

    /**
     * Send the given data to the remote
     * <p>
     * The offset defaults to 0, and the length defaults to the length of the bytes.
     *
     * @param flags The custom {@link Flags} for this operation.
     * @param bytes To read from.
     * @throws IOException If an IO error occurs, or {@link CancelledException} if the operation is cancelled.
     * @see #reply(long, byte[], int, int)
     */
    public void reply(long flags, byte[] bytes) throws IOException
    {
        reply(0, bytes, 0, bytes.length);
    }

    /**
     * Send the given data to the remote.
     * <p>
     * The bytes and the length default to the string data.
     *
     * @param flags  The custom {@link Flags} for this operation.
     * @param bytes  To read from.
     * @param offset The length of bytes to skip from start.
     * @param length The length of the bytes to read and send.
     * @throws IOException If an IO error occurs, or {@link CancelledException} if the operation is cancelled.
     */
    public void reply(long flags, byte[] bytes, int offset, int length) throws IOException
    {
        Description description = writeBegin(0, bytes.length);
        write(description, bytes, offset, length);
        writeEnd(description);
    }

    /**
     * Send data reading from an {@link InputStream} to the remote where all the data will be read until it returns -1
     * or gets closed during the read.
     * <p>
     * This will not inform the remote about the size of the data. Instead, it will send the size as it reads from the
     * input stream.
     * <p>
     * If it gets closed during the read, you may think of calling {@link #cancel()} so that we can send a signal to the
     * remote that this operation is no longer valid.
     * <p>
     * You may choose to use {@link #reply(long, InputStream, long)} if the length of the data is known.
     *
     * @param flags       The custom {@link Flags} for this operation.
     * @param inputStream To read from.
     * @throws IOException If an IO error occurs, or {@link CancelledException} if the operation is cancelled.
     * @see #reply(String)
     * @see #reply(long, InputStream, long)
     */
    public void reply(long flags, InputStream inputStream) throws IOException
    {
        reply(flags, inputStream, CoolSocket.LENGTH_UNSPECIFIED);
    }

    /**
     * Send to the remote from an {@link InputStream} where all the data will be read until it returns -1 or gets closed
     * during the read.
     * <p>
     * The size of the data is reported before the write starts and it will not send more than that even if the input
     * stream has data available.
     * <p>
     * Also, the remote will not read more than the size that we reported.
     * <p>
     * If the data length is unknown, use {@link #reply(long, InputStream)} which won't need that
     * information.
     *
     * @param flags       The custom {@link Flags} for this operation.
     * @param inputStream To read from.
     * @param fixedSize   The length of the data when complete.
     * @throws IOException If an IO error occurs, or {@link CancelledException} if the operation is cancelled.
     */
    public void reply(long flags, InputStream inputStream, long fixedSize) throws IOException
    {
        Description description = writeBegin(flags, fixedSize);
        write(description, inputStream);
        writeEnd(description);
    }

    /**
     * Set the maximum length when reading from the remote with {@link #receive()}.
     * <p>
     * This will not be used for custom output streams, i.e. {@link #receive(OutputStream, int)}.
     *
     * @param internalCacheLimit The limit in bytes.
     * @see #receive()
     */
    public void setInternalCacheLimit(int internalCacheLimit)
    {
        this.internalCacheLimit = internalCacheLimit;
    }

    /**
     * Write to the remote.
     * <p>
     * The offset defaults to 0, and the length defaults to the length of byte array.
     *
     * @param description The description object representing the operation.
     * @param bytes       To read from.
     * @throws IOException If an IO error occurs, {@link CancelledException} if the operation is cancelled.
     * @see #write(Description, byte[], int, int)
     */
    public void write(Description description, byte[] bytes) throws IOException
    {
        write(description, bytes, 0, bytes.length);
    }

    /**
     * Write to the remote.
     * <p>
     * The buffer defaults to the internal buffer {@link Description#buffer}.
     *
     * @param description The description object representing the operation.
     * @param offset      The bytes to skip reading from the bytes.
     * @param length      The length of how much to read from the bytes.
     * @throws IOException If an IO error occurs, or {@link CancelledException} if the operation is cancelled.
     * @see #write(Description, byte[], int, int)
     */
    public void write(Description description, int offset, int length) throws IOException
    {
        write(description, description.buffer, offset, length);
    }

    /**
     * Write to the remote.
     * <p>
     * This should be invoked after {@link #writeBegin(long, long)} which will inform the remote about the operation.
     * <p>
     * The remote should be waiting for this by calling one of the read or receive methods.
     * <p>
     * When all the data is sent, you should invoke the {@link #writeEnd(Description)} method so that the remote can
     * know no data incoming left in the case of {@link Flags#chunked()}.
     *
     * @param description The description object representing the operation.
     * @param bytes       To read from.
     * @param offset      The bytes to skip reading from the bytes.
     * @param length      The length of how much to read from the bytes.
     * @throws IOException If an IO error occurs, or {@link CancelledException} if the operation is cancelled.
     * @see #writeBegin(long, long)
     * @see #write(Description, int, int)
     * @see #write(Description, byte[], int, int)
     * @see #write(Description, InputStream)
     * @see #writeEnd(Description)
     */
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
            throw new SizeExceededException("The size of the data exceeds that length notified to the remote.",
                    description.totalLength, description.handedLength);

        handleByteBreak(description, true);
        writeSize(size);

        getOutputStreamPriv().write(bytes, offset, length);
    }

    /**
     * Write all the data read from the input stream.
     *
     * @param description The description object representing the operation.
     * @param inputStream To read from.
     * @throws IOException If an IO error occurs, or {@link CancelledException} if the operation is cancelled.
     */
    public synchronized void write(Description description, InputStream inputStream) throws IOException
    {
        int len;
        while ((len = inputStream.read(description.buffer)) != -1) {
            write(description, 0, len);
        }
    }

    /**
     * Prepare for sending a response. This will send the flags for the upcoming data transmission so that the remote
     * will know how to treat the data it receives.
     * <p>
     * After this method call, you should invoke {@link #write(Description, byte[], int, int)} to begin writing bytes
     * or end the part with the {@link #writeEnd(Description)} method call.
     *
     * @param flags       The feature flags set for this part. It sets how the remote should handle the data it
     * @param totalLength The total length of the data that will be sent. Use {@link CoolSocket#LENGTH_UNSPECIFIED} when
     *                    the length is unknown at the moment. Doing so will make this transmission process
     *                    {@link Flags#FLAG_DATA_CHUNKED} where the data length will only be visible as much as
     *                    we read from the source.
     * @return The description object for this write operation.
     * @throws IOException When socket related IO error occurs.
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

    /**
     * Write byte.
     *
     * @param b The byte to write.
     * @throws IOException If an IO error occurs.
     */
    protected void writeByte(int b) throws IOException
    {
        getOutputStreamPriv().write(b);
    }

    /**
     * Write {@link ByteBreak} byte.
     *
     * @param byteBreak To write.
     * @throws IOException If an IO error occurs.
     */
    protected void writeByteBreak(ByteBreak byteBreak) throws IOException
    {
        writeByte(byteBreak.ordinal());
    }

    /**
     * Finalize the write operation that was started with {@link #writeBegin(long, long)}.
     *
     * @param description The description object representing the operation.
     * @throws IOException If an IO error occurs, or {@link CancelledException} if the operation is cancelled.
     */
    public synchronized void writeEnd(Description description) throws IOException
    {
        if (description.flags.chunked()) {
            handleByteBreak(description, true);
            writeSize(-1);
        }

        getOutputStreamPriv().flush();
    }

    /**
     * Write {@link Flags}.
     *
     * @param flags The long integer for the flags.
     * @throws IOException If an IO error occurs.
     */
    protected void writeFlags(long flags) throws IOException
    {
        writeSize(flags);
    }

    /**
     * Write an integer.
     *
     * @param value The integer to write.
     * @throws IOException If an IO error occurs.
     */
    protected void writeInteger(int value) throws IOException
    {
        getOutputStreamPriv().write(ByteBuffer.allocate(Integer.BYTES).putInt(value).array());
    }

    /**
     * Write a long integer.
     *
     * @param value The long integer to write.
     * @throws IOException If an IO error occurs.
     */
    protected void writeLong(long value) throws IOException
    {
        getOutputStreamPriv().write(ByteBuffer.allocate(Long.BYTES).putLong(value).array());
    }

    /**
     * Write a size that is in long integer format.
     *
     * @param size To write.
     * @throws IOException If an IO error occurs.
     */
    protected void writeSize(long size) throws IOException
    {
        writeLong(size);
    }

    /**
     * A description explains how an read/write operation will be handled, and how much it has progressed.
     * <p>
     * This also holds internal buffer so that the same buffer can be used without occupying more space on the memory.
     */
    public static class Description
    {
        /**
         * Flags are created by {@link #readBegin()} or {@link #writeBegin(long, long)} and explain the conditions
         * of the operation.
         */
        public final Flags flags;

        /**
         * The internal buffer that is used to copy bytes as we read from/write to the remote.
         */
        public final byte[] buffer;

        /**
         * This is filled as we read or write to the remote. If this is not a chunked transfer {@link Flags#chunked()},
         * we make use of it by subtracting it from the {@link #totalLength}.
         */
        protected long handedLength;

        /**
         * The total length to be delivered when the operation is complete.
         * <p>
         * If {@link Flags#chunked()}, it will be equal to zero. It will have the final value when the operation is
         * complete.
         */
        protected long totalLength;

        /**
         * The reported size from the remote that should be read before reading another size {@link #readSize(byte[])}.
         * <p>
         * This will be used by the read operations so that we can know when the remote sends {@link ByteBreak}.
         */
        protected long awaitingSize;

        /**
         * The byte break operation that this side wants to execute.
         */
        private ByteBreak byteBreakLocal = null;

        /**
         * The byte break operation that the remtoe side wants to execute.
         */
        private ByteBreak byteBreakRemote = null;

        /**
         * If the decided operation is {@link InfoExchange} via {@link ByteBreak#InfoExchange}, this will hold the
         * value for what should be executed, and after it's executed, it will be cleared.
         */
        private InfoExchange pendingExchange = null;

        /**
         * Create a new instance.
         * <p>
         * The flags are encapsulated in a {@link Flags} instance.
         *
         * @param flags       The long integer representing the flags.
         * @param totalLength The total length of the operation. If it is {@link CoolSocket#LENGTH_UNSPECIFIED}, then
         *                    it will be 0 when passed on to the {@link #totalLength} field.
         * @param buffer      To cache the read or written data.
         */
        public Description(long flags, long totalLength, byte[] buffer)
        {
            this(new Flags(flags), totalLength, buffer);
        }

        /**
         * Create a new instance.
         *
         * @param flags       The flags for this operation.
         * @param totalLength The total length of the operation. If it is {@link CoolSocket#LENGTH_UNSPECIFIED}, then
         *                    it will be 0 when passed on to the {@link #totalLength} field.
         * @param buffer      To cache the read or written data.
         */
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

        /**
         * Check whether there is more data available for use. This may be unreliable for chunked transfers.
         *
         * @return The latest readable data if this is a chunked operation and that value will be changing as the
         * read operations will consume that value, or the length that is yet to be read for an operation with a fixed
         * length. That value will be {@link CoolSocket#LENGTH_UNSPECIFIED} when {@link #handedLength} exceeds
         * {@link #totalLength} to indicate next reads will not bring more data.
         */
        public long available()
        {
            return flags.chunked() ? awaitingSize
                    : (totalLength <= handedLength ? CoolSocket.LENGTH_UNSPECIFIED : totalLength - handedLength);
        }

        /**
         * Check whether there is more data to come.
         *
         * @return True if there may be more data incoming.
         */
        public boolean hasAvailable()
        {
            return available() != CoolSocket.LENGTH_UNSPECIFIED;
        }
    }
}