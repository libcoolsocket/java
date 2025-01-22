package org.monora.coolsocket.core.session;

import org.jetbrains.annotations.NotNull;
import org.monora.coolsocket.core.CoolSocket;
import org.monora.coolsocket.core.config.Config;
import org.monora.coolsocket.core.response.*;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;

import static org.monora.coolsocket.core.CoolSocket.LENGTH_UNSPECIFIED;
import static org.monora.coolsocket.core.config.Config.DEFAULT_BUFFER_SIZE;
import static org.monora.coolsocket.core.config.Config.DEFAULT_INVERSE_EXCHANGE_POINT;

/**
 * The wrapper that transforms messages into CoolSocket packets.
 * <p>
 * This class doesn't require a CoolSocket server to function. It can wrap any valid {@link Socket} instance.
 *
 * @see Channel#wrap(Socket)
 */
public class Channel implements Closeable {
    /**
     * The wrapped socket.
     */
    private final Socket socket;

    /**
     * The output stream of the socket.
     */
    private final OutputStream outputStream;

    /**
     * The writable channel for the socket.
     */
    private final WritableByteChannel writableByteChannel;

    /**
     * The readable channel for the socket.
     */
    private final ReadableByteChannel readableByteChannel;

    /**
     * The buffer size for read operations where an internal buffer is used.
     */
    private int defaultBufferSize = DEFAULT_BUFFER_SIZE;

    /**
     * The next descriptor id.
     */
    private int nextOperationId = 0;

    /**
     * The protocol version reported by the remote. This will not be available until the first communication.
     */
    private int protocolVersion;

    /**
     * Whether there is a pending cancellation request that needs sending to the remote.
     */
    private boolean pendingCancellation;

    /**
     * Whether there is a pending close request that needs sending to the remote.
     */
    private boolean pendingClose;

    /**
     * Whether multichannel mode is enabled. When enabled, read and write pipelines are separated.
     */
    private boolean multichannel = false;

    /**
     * Whether this channel is roaming.
     * <p>
     * If roaming, it will not be closed when the server goes out of scope and the channel will need closing explicitly.
     */
    private boolean roaming = false;

    /**
     * Create a new instance.
     *
     * @param socket       To wrap.
     * @param inputStream  Of the socket to read from.
     * @param outputStream Of the socket to write to.
     */
    Channel(@NotNull Socket socket, @NotNull InputStream inputStream, @NotNull OutputStream outputStream) {
        this.socket = socket;
        this.outputStream = outputStream;
        this.readableByteChannel = Channels.newChannel(inputStream);
        this.writableByteChannel = Channels.newChannel(outputStream);
    }

    /**
     * Wrap an active socket to start messaging using CoolSocket protocol.
     *
     * @param socket To communicate over.
     * @return The channel.
     * @throws IOException If the socket is not valid.
     */
    public static Channel wrap(@NotNull Socket socket) throws IOException {
        checkSocket(socket);
        return new Channel(socket, socket.getInputStream(), socket.getOutputStream());
    }

    /**
     * Check if the given range is in bounds.
     *
     * @param totalLength The total available length.
     * @param offset      The length skip past from the start.
     * @param length      The length that needs reading.
     * @throws IndexOutOfBoundsException If the given range is out of bounds.
     */
    private static void checkBounds(int totalLength, int offset, int length) throws IndexOutOfBoundsException {
        if (length < 0 || offset < 0 || offset + length > totalLength) {
            throw new IndexOutOfBoundsException("The data point is not valid.");
        }
    }

    /**
     * Check whether the socket is still, or throw an error.
     *
     * @param socket To check.
     * @throws IllegalStateException If the socket is not valid.
     */
    private static void checkSocket(@NotNull Socket socket) {
        if (!socket.isConnected()) {
            throw new IllegalStateException("Socket should have a valid connection.");
        }
    }

    /**
     * Close the socket.
     *
     * @throws IOException If an IO error occurs.
     */
    @Override
    public void close() throws IOException {
        socket.close();
    }

    /**
     * Schedule the closing of the socket by reporting to the remote first.
     * <p>
     * This will need another read/write operation to report the request to the remote.
     *
     * @throws IOException If the socket is already closed.
     */
    public void closeMutually() throws IOException {
        if (getSocket().isClosed())
            throw new IOException("Socket is already closed.");
        pendingClose = true;
    }

    /**
     * Schedule the cancellation of the next read/write operation.
     * <p>
     * This will need another read/write operation to report the request to the remote.
     *
     * @throws IOException If the socket is already closed.
     */
    public void cancel() throws IOException {
        if (getSocket().isClosed())
            throw new IOException("Socket is closed.");
        pendingCancellation = true;
    }

    /**
     * Whether there is a pending cancellation request.
     *
     * @param clear True to the clear the request and cancel the request if there is one.
     * @return True if there is a pending cancellation request.
     */
    public boolean isWaitingCancellation(boolean clear) {
        if (pendingCancellation) {
            if (clear) pendingCancellation = false;
            return true;
        }
        return false;
    }

    /**
     * Whether there is a pending close request.
     *
     * @return If there is one.
     */
    public boolean isWaitingToCloseMutually() {
        return pendingClose;
    }

    /**
     * The default buffer size for read operations where max length is not provided.
     *
     * @return The default buffer size.
     * @see #setDefaultBufferSize(int)
     */
    public int getDefaultBufferSize() {
        return defaultBufferSize;
    }

    /**
     * Set the default buffer size for read operations where max length is not specified.
     * <p>
     * This will affect the next operations only.
     *
     * @param defaultBufferSize To apply.
     * @see #getDefaultBufferSize()
     */
    public void setDefaultBufferSize(int defaultBufferSize) {
        this.defaultBufferSize = defaultBufferSize;
    }

    /**
     * The protocol version reported by remote.
     *
     * @return The protocol version.
     */
    public int getProtocolVersion() {
        return protocolVersion;
    }

    /**
     * The socket instance used for messaging.
     *
     * @return The socket instance.
     */
    public @NotNull Socket getSocket() {
        return socket;
    }

    /**
     * Whether multichannel mode is activated.
     *
     * @return True if multichannel mode is activated.
     * @see #setMultichannel(boolean)
     */
    public boolean isMultichannel() {
        return multichannel;
    }

    /**
     * Set whether multichannel mode is activated.
     * <p>
     * When multichannel mode is activated, read and write channels become separated, and both can be used
     * asynchronously. Note that enabling this disables cancellation and mutual close requests.
     * <p>
     * Both sides have to enable or disable this at the same time. Otherwise, it will cause errors.
     *
     * @param multichannel True to enable multichannel request.
     * @see #isMultichannel()
     */
    public void setMultichannel(boolean multichannel) {
        this.multichannel = multichannel;
    }

    /**
     * Whether this channel is roaming.
     *
     * @return True if roaming
     * @see #setRoaming(boolean)
     */
    public boolean isRoaming() {
        return roaming;
    }

    /**
     * Set the roaming mode.
     * <p>
     * Roaming mode affects how CoolSocket server instance treats this channel.
     * <p>
     * When roaming, the server will not close this channel when it goes out of scope.
     * <p>
     * This is useful when you want to move the communication to another thread and let the server thread handle this
     * channel to exit.
     *
     * @param roaming True to enable roaming.
     */
    public void setRoaming(boolean roaming) {
        this.roaming = roaming;
    }

    /**
     * See {@link #readBegin(int, int)} for more info.
     *
     * @return To read from.
     * @throws IOException If an IO error occurs.
     */
    public @NotNull ReadableDescriptor readBegin() throws IOException {
        return readBegin(DEFAULT_BUFFER_SIZE, DEFAULT_INVERSE_EXCHANGE_POINT);
    }

    /**
     * Begin reading from remote.
     * <p>
     * The consecutive reads should be performed using {@link ReadableDescriptor#read()}.
     * <p>
     * The remote should be ready to call {@link #writeBegin} for this to work.
     *
     * @param bufferSize           The size of the buffer.
     * @param inverseExchangePoint After how many operations to exchange pending requests.
     * @return To read from.
     * @throws IOException If an IO error occurs while preparing the read operation.
     * @see #writeBegin
     */
    public @NotNull ReadableDescriptor readBegin(int bufferSize, int inverseExchangePoint) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(bufferSize);

        if (multichannel) {
            inverseExchangePoint = DEFAULT_INVERSE_EXCHANGE_POINT;
        } else {
            byteBuffer.putInt(inverseExchangePoint).flip();
            writableByteChannel.write(byteBuffer);
        }

        readOrFail(byteBuffer, Long.BYTES * 2 + Integer.BYTES);

        ReadableDescriptor descriptor = new ReadableDescriptor(new Flags(byteBuffer.getLong()), byteBuffer.getInt(), byteBuffer.getLong(),
                inverseExchangePoint, byteBuffer);

        nextOperationId = descriptor.operationId;

        descriptor.readState();
        if (!multichannel) descriptor.writeState();

        return descriptor;
    }

    /**
     * Read the given length of data or fail with {@link SocketException}.
     *
     * @param byteBuffer To read into (cleared before being used).
     * @param length     The length of the data to read from the remote.
     * @throws IOException If an IO error occurs, i.e., fails to read the right amount of data, or the given length is
     *                     larger than the byte buffer.
     */
    protected void readOrFail(@NotNull ByteBuffer byteBuffer, int length) throws IOException {

        byteBuffer.clear();
        byteBuffer.limit(length);

        while (byteBuffer.hasRemaining())
            if (readableByteChannel.read(byteBuffer) == -1)
                break;

        if (byteBuffer.hasRemaining())
            throw new SocketException("Socket is closed or could not read " + length + " data in length.");

        byteBuffer.flip();
    }

    /**
     * See {@link #readAll(int)} for more info.
     *
     * @return The response.
     * @throws IOException If an IO error occurs.
     */
    public @NotNull Response readAll() throws IOException {
        return readAll(getDefaultBufferSize());
    }

    /**
     * Read a packet from the remote.
     *
     * @param maxLength A positive number to apply a limit. If enabled and then exceeded, this will throw an
     *                  {@link SizeOverflowException}
     * @return The response.
     * @throws IOException If an IO error occurs.
     */
    public @NotNull Response readAll(int maxLength) throws IOException {
        int len;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        WritableByteChannel writableByteChannel = Channels.newChannel(outputStream);
        ReadableDescriptor descriptor = readBegin();

        do {
            len = descriptor.read();

            if (maxLength > 0 && descriptor.consumedLength > maxLength)
                throw new SizeOverflowException("The length of the data exceeds the maximum length.", maxLength,
                        descriptor.consumedLength);

            if (len > 0)
                writableByteChannel.write(descriptor.byteBuffer);
        } while (descriptor.hasAvailable());

        return new Response(getSocket().getRemoteSocketAddress(), descriptor.flags, descriptor.totalLength,
                outputStream);
    }

    /**
     * See {@link #writeBegin(long, long)} for more info.
     *
     * @param flags The flags for this operation. See {@link Flags}.
     * @return The descriptor to write to.
     * @throws IOException If an IO error occurs.
     */
    public @NotNull WritableDescriptor writeBegin(long flags) throws IOException {
        return writeBegin(flags | Flags.FLAG_DATA_CHUNKED, 0);
    }

    /**
     * Begin writing to remote.
     * <p>
     * The consecutive writes should be performed using {@link WritableDescriptor#write}.
     * <p>
     * The remote should be ready to call {@link #readBegin} for this to work.
     *
     * @param flags       The flags valid for this operation. See {@link Flags}.
     * @param totalLength The total length of the operation or {@link CoolSocket#LENGTH_UNSPECIFIED} if unknown.
     * @return The descriptor to write to.
     * @throws IOException If an IO error occurs while preparing the read operation.
     * @see #writeBegin
     */
    public @NotNull WritableDescriptor writeBegin(long flags, long totalLength) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
        int operationId = ++nextOperationId;

        int inverseExchangePoint;

        if (multichannel) {
            inverseExchangePoint = DEFAULT_INVERSE_EXCHANGE_POINT;
        } else {
            readOrFail(byteBuffer, Integer.BYTES);
            inverseExchangePoint = byteBuffer.getInt();
            byteBuffer.clear();
        }

        WritableDescriptor descriptor = new WritableDescriptor(new Flags(flags), operationId, totalLength, inverseExchangePoint, byteBuffer);
        byteBuffer.putLong(flags)
                .putInt(operationId)
                .putLong(totalLength)
                .flip();

        writableByteChannel.write(byteBuffer);
        byteBuffer.clear();

        descriptor.writeState();
        if (!multichannel) descriptor.readState();

        return descriptor;
    }

    /**
     * Write the given bytes to the remote.
     *
     * @param bytes To write.
     * @throws IOException If an IO error occurs.
     * @see #writeAll(byte[], int, int)
     */
    public void writeAll(byte[] bytes) throws IOException {
        writeAll(bytes, 0, bytes.length);
    }

    /**
     * Write the data within the requested boundary from the given byte array.
     * <p>
     * The remote should be ready to call any of the read functions.
     *
     * @param bytes  To write.
     * @param offset To offset from the start.
     * @param length The total length of data to read and send.
     * @throws IOException If an IO error occurs, or if the requested data is out of bounds.
     */
    public void writeAll(byte[] bytes, int offset, int length) throws IOException {
        checkBounds(bytes.length, offset, length);

        WritableDescriptor descriptor = writeBegin(0, length);
        descriptor.write(bytes, offset, length);
        descriptor.writeEnd();
    }

    /**
     * Write the all data read from the given input stream.
     *
     * @param inputStream To read from.
     * @throws IOException If an IO error occurs.
     * @see #writeAll(InputStream, long)
     */
    public void writeAll(@NotNull InputStream inputStream) throws IOException {
        writeAll(inputStream, LENGTH_UNSPECIFIED);
    }

    /**
     * Write the data read from the given input stream.
     * <p>
     * Teh remote should be ready to call any of the read functions.
     *
     * @param inputStream To read from.
     * @param fixedSize   The exact length of data to read from the input stream.
     * @throws IOException If an IO error occurs, or if the input stream has fewer data than requested to write.
     */
    public void writeAll(@NotNull InputStream inputStream, long fixedSize) throws IOException {
        WritableDescriptor descriptor = writeBegin(fixedSize == LENGTH_UNSPECIFIED ? Flags.FLAG_DATA_CHUNKED : 0, fixedSize == LENGTH_UNSPECIFIED ? 0 : fixedSize);
        descriptor.write(inputStream);
        descriptor.writeEnd();
    }

    /**
     * A CoolSocket packet.
     */
    public abstract class Descriptor {
        /**
         * The flags valid for this operation.
         */
        public final @NotNull Flags flags;

        /**
         * The unique identifier for this operation.
         * <p>
         * This is also used to verify integrity of the packets.
         */
        public final int operationId;

        /**
         * The point in cycle on which the receiver will be sending the sender a {@link ProtocolRequest}.
         * <p>
         * This determines whether a description is going to be more communicative or more performant.
         * <p>
         * In other words, if the receiver sends messages more frequently to the sender, the sender will have
         * to do blocking to receive those messages, which will impact the performance negatively.
         * <p>
         * For the sake of simplicity, this isn't alterable once set for a description.
         */
        public final int inverseExchangePoint;

        /**
         * The byte buffer that manages transferring bytes.
         */
        public final @NotNull ByteBuffer byteBuffer;

        /**
         * This is filled as we read or write to the remote. If this is not a chunked transfer {@link Flags#chunked()},
         * we make use of it by subtracting it from the {@link #totalLength}.
         */
        protected long consumedLength;

        /**
         * The total length to be delivered when the operation is complete.
         * <p>
         * If {@link Flags#chunked()}, it will be equal to zero. It will have the final value when the operation is
         * complete.
         */
        protected long totalLength;

        /**
         * The reported size from the remote that should be read before reading another size.
         * <p>
         * This will be used by the read operations so that we can know when the remote sends {@link ProtocolRequest}.
         */
        protected long nextAvailable;

        /**
         * The internal transaction count that is used to decide when to write information bytes during read or the
         * opposite.
         */
        protected int transactionCount;

        /**
         * Create a new instance.
         *
         * @param flags                The flags for this operation.
         * @param operationId          The unique id for this descriptor.
         * @param totalLength          The total length of the data if available, or {@link CoolSocket#LENGTH_UNSPECIFIED}.
         * @param inverseExchangePoint The point where the writer will read and the reader will write state.
         * @param byteBuffer           The internal buffer to read from and write into.
         */
        protected Descriptor(@NotNull Flags flags, int operationId, long totalLength, int inverseExchangePoint,
                             @NotNull ByteBuffer byteBuffer) {
            if (byteBuffer.capacity() < DEFAULT_BUFFER_SIZE)
                throw new BufferUnderflowException();

            if (totalLength < 0)
                throw new IllegalArgumentException("Total length cannot be negative number.");

            if (inverseExchangePoint < 1)
                throw new IllegalArgumentException("Inverse exchange point cannot be 0 or a negative number.");

            this.flags = flags;
            this.operationId = operationId;
            this.totalLength = totalLength;
            this.inverseExchangePoint = inverseExchangePoint;
            this.byteBuffer = byteBuffer;
        }

        /**
         * Return the available data length.
         * <p>
         * If chunked, this will return the data length available. This will usually not reflect the whole as the
         * data is available as much as it is read.
         * <p>
         * If not chunked, this will return the data length left to be consumed as a whole.
         * <p>
         * If this returns {@link CoolSocket#LENGTH_UNSPECIFIED}, it will mean no data available and this description
         * is closed. You can also use {@link #hasAvailable()} to check if there is data available.
         *
         * @return The available data length.
         * @see #hasAvailable()
         */
        public long available() {
            return flags.chunked() ? nextAvailable : (totalLength <= consumedLength ? LENGTH_UNSPECIFIED
                    : totalLength - consumedLength);
        }

        /**
         * Get the data length that has been moved so far.
         *
         * @return The moved data length.
         */
        public long consumedLength() {
            return consumedLength;
        }

        /**
         * Check whether there is more data to come.
         *
         * @return True if there may be more data to transfer, or false if the description is no longer valid to use
         * for reading/writing.
         */
        public boolean hasAvailable() {
            return available() != LENGTH_UNSPECIFIED;
        }

        /**
         * Whether the descriptor is for zero-length data.
         *
         * @return True if the total length of the data is known and is 0.
         */
        protected boolean isZeroLength() {
            return !flags.chunked() && totalLength == 0;
        }

        /**
         * Get the total length for this operation.
         * <p>
         * To check whether there is more to come, use {@link #hasAvailable()}.
         * <p>
         * Chunked operations will show the length that has been moved. If not completed yet, the shown value will
         * also be incomplete.
         *
         * @return The calculated data length that has been transferred.
         * @see #hasAvailable()
         */
        public long totalLength() {
            return totalLength;
        }

        /**
         * Receive the state from the remote.
         *
         * @return The exchanged info.
         * @throws IOException If an IO error occurs.
         */
        private InfoExchange exchangeReceive() throws IOException {
            readOrFail(byteBuffer, Integer.BYTES);
            InfoExchange infoExchange = InfoExchange.from(byteBuffer.getInt());

            if (Objects.requireNonNull(infoExchange) == InfoExchange.ProtocolVersion) {
                readOrFail(byteBuffer, Integer.BYTES);
                protocolVersion = byteBuffer.getInt();
            }

            return infoExchange;
        }

        /**
         * Send the state to the remote.
         *
         * @param infoExchange The info to exchange.
         * @throws IOException If an IO error occurs.
         */
        private void exchangeSend(@NotNull InfoExchange infoExchange) throws IOException {
            byteBuffer.clear();
            byteBuffer.putInt(infoExchange.ordinal());

            if (infoExchange == InfoExchange.ProtocolVersion) {
                byteBuffer.putInt(Config.PROTOCOL_VERSION);
            } else {
                throw new UnsupportedFeatureException("Requested feature is unsupported.");
            }

            byteBuffer.flip();
            writableByteChannel.write(byteBuffer);
        }

        /**
         * Exchange info with the remote and perform the most suitable action.
         *
         * @param write Whether to write or read.
         * @throws IOException If an IO error occurs.
         */
        private void handleProtocolRequest(boolean write) throws IOException {
            InfoExchange exchange = null;
            ProtocolRequest protocolRequest;

            if (write) {
                if (isWaitingToCloseMutually())
                    protocolRequest = ProtocolRequest.Close;
                else if (isWaitingCancellation(true))
                    protocolRequest = ProtocolRequest.Cancel;
                else if (protocolVersion == 0 && !multichannel) {
                    protocolRequest = ProtocolRequest.InfoExchange;
                    exchange = InfoExchange.ProtocolVersion;
                } else
                    protocolRequest = ProtocolRequest.None;

                byteBuffer.clear();
                byteBuffer.putInt(operationId)
                        .putInt(protocolRequest.ordinal())
                        .flip();
                writableByteChannel.write(byteBuffer);
            } else {
                readOrFail(byteBuffer, Integer.BYTES * 2);
                int remoteOperationId = byteBuffer.getInt();
                protocolRequest = ProtocolRequest.from(byteBuffer.getInt());
                if (operationId != remoteOperationId)
                    throw new DescriptorMismatchException("The remote description is different than ours.", this,
                            remoteOperationId);
            }

            switch (protocolRequest) {
                case Close:
                    try {
                        close();
                    } catch (Exception ignored) {
                    }
                    throw new ClosedException("The connection closed.", !write);
                case Cancel:
                    throw new CancelledException("This operation has been cancelled.", !write);
                case InfoExchange:
                    if (write) {
                        exchangeSend(exchange);
                        exchangeReceive();
                    } else
                        exchangeSend(exchangeReceive());
                    break;
                case None:
                default:
                    return;
            }

            handleProtocolRequest(write);
        }

        /**
         * Read the state.
         *
         * @throws IOException If an IO error occurs.
         */
        protected void readState() throws IOException {
            handleProtocolRequest(false);
        }

        /**
         * Write the state.
         *
         * @throws IOException If an IO error occurs.
         */
        protected void writeState() throws IOException {
            handleProtocolRequest(true);
        }

        /**
         * Verify that the descriptor can still read/write data.
         *
         * @throws DescriptorClosedException If the description is closed.
         */
        public void verify() throws DescriptorClosedException {
            if (!hasAvailable()) {
                throw new DescriptorClosedException("This description is closed.", this);
            }
        }
    }

    /**
     * A CoolSocket packet to read from.
     */
    public class ReadableDescriptor extends Descriptor {
        /**
         * Create a new instance.
         *
         * @param flags                The flags for this operation.
         * @param operationId          The unique id for this descriptor.
         * @param totalLength          The total length of the data if available, or {@link CoolSocket#LENGTH_UNSPECIFIED}.
         * @param inverseExchangePoint The point where the writer will read and the reader will write state.
         * @param byteBuffer           The internal buffer to read from and write into.
         */
        protected ReadableDescriptor(Flags flags, int operationId, long totalLength, int inverseExchangePoint, ByteBuffer byteBuffer) {
            super(flags, operationId, totalLength, inverseExchangePoint, byteBuffer);
        }

        /**
         * Read all the data sent by the remote.
         *
         * @return The length of data that has been read.
         * @throws IOException If an IO error occurs, or the input stream closes/ends before reading all the data.
         */
        public int read() throws IOException {
            if (isZeroLength()) {
                return 0;
            }

            verify();
            boolean chunked = flags.chunked();

            if (nextAvailable <= 0) {
                if (!multichannel && transactionCount++ == inverseExchangePoint) {
                    writeState();
                    transactionCount = 0;
                } else {
                    readState();
                }
                readOrFail(byteBuffer, Long.BYTES);
                nextAvailable = byteBuffer.getLong();

                if (nextAvailable == LENGTH_UNSPECIFIED) {
                    if (hasAvailable())
                        throw new SizeUnderflowException("Remote closed the connection before reading the data in " +
                                "full.", totalLength, consumedLength);
                    else
                        return LENGTH_UNSPECIFIED;
                }
            }

            byteBuffer.clear();
            int length = (int) Math.min(byteBuffer.remaining(),
                    Math.min(nextAvailable, available()));
            byteBuffer.limit(length);
            length = readableByteChannel.read(byteBuffer);
            byteBuffer.flip();

            consumedLength += length;
            nextAvailable -= length;

            if (chunked)
                totalLength += length;

            return length;
        }
    }

    /**
     * A CoolSocket packet to write to.
     */
    public class WritableDescriptor extends Descriptor {
        /**
         * Create a new instance.
         *
         * @param flags                The flags for this operation.
         * @param operationId          The unique id for this descriptor.
         * @param totalLength          The total length of the data if available, or {@link CoolSocket#LENGTH_UNSPECIFIED}.
         * @param inverseExchangePoint The point where the writer will read and the reader will write state.
         * @param byteBuffer           The internal buffer to read from and write into.
         */
        protected WritableDescriptor(Flags flags, int operationId, long totalLength, int inverseExchangePoint, ByteBuffer byteBuffer) {
            super(flags, operationId, totalLength, inverseExchangePoint, byteBuffer);
        }

        /**
         * Write the given bytes.
         *
         * @param bytes To write
         * @throws IOException If an IO error occurs.
         * @see #write(byte[], int, int)
         */
        public void write(byte[] bytes) throws IOException {
            write(bytes, 0, bytes.length);
        }

        /**
         * Write the data within the boundary from the given bytes.
         *
         * @param bytes  To write.
         * @param offset To offset from the start.
         * @param length The length to read from the bytes and then write.
         * @throws IOException If an IO error occurs, or if the requested data is out of boundary.
         */
        public void write(byte[] bytes, int offset, int length)
                throws IOException {
            if (isZeroLength()) {
                return;
            }

            verify();

            checkBounds(bytes.length, offset, length);

            boolean chunked = flags.chunked();
            int consume = length - offset;

            if (!chunked && consume > available()) {
                throw new SizeOverflowException("Trying write more than the value reported to the remote.",
                        available(), consume);
            }

            if (!multichannel && transactionCount++ == inverseExchangePoint) {
                readState();
                transactionCount = 0;
            } else
                writeState();

            if (chunked)
                totalLength += consume;

            consumedLength += consume;

            byteBuffer.clear();
            byteBuffer.putLong(consume);
            byteBuffer.flip();
            writableByteChannel.write(byteBuffer);
            outputStream.write(bytes, offset, length);
        }

        /**
         * Write all the data read from the given input stream.
         *
         * @param inputStream To read from.
         * @throws IOException If an IO error occurs.
         */
        public void write(@NotNull InputStream inputStream) throws IOException {
            int len;
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            while ((len = inputStream.read(buffer)) != -1) {
                write(buffer, 0, len);
            }
        }

        /**
         * End the write operation, and report to remote if needed.
         * <p>
         * If the total length is known, calling this won't have an effect, however, if you call this early when
         * the length is known, this will throw a {@link SizeUnderflowException} which will need catching.
         * <p>
         * If the length is unknown, this will inform the remote that the write operation has ended.
         *
         * @throws IOException            If an IO error occurs.
         * @throws SizeUnderflowException If the size is known, and this is called before write operation finishes.
         */
        public void writeEnd() throws IOException {
            if (!hasAvailable())
                return;

            writeState();
            byteBuffer.clear();
            byteBuffer.putLong(LENGTH_UNSPECIFIED);
            byteBuffer.flip();
            writableByteChannel.write(byteBuffer);

            nextAvailable = LENGTH_UNSPECIFIED;

            outputStream.flush();

            // If not chunked, then the size must be known, and if the transferred size is smaller than reported, this
            // is an error.
            if (!flags.chunked())
                throw new SizeUnderflowException("The write operation should not be ended. The written byte length" +
                        " is below what was reported.", totalLength, consumedLength);
        }
    }
}
