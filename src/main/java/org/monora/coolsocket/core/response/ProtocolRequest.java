package org.monora.coolsocket.core.response;

import org.jetbrains.annotations.NotNull;

/**
 * Protocol requests are a way of exchanging information between two peers.
 * <p>
 * They are also sent and received to ensure the bidirectional communication between the two sides still occurs even
 * when the one directional read-write is the case, i.e. when you're writing but not reading.
 * <p>
 * They are also exchanged during the beginning of a read or write operation, or when there is a read and write
 * operation taking place.
 * <p>
 * The cancel request, for instance, is a protocol request operation that happens during any of the read or write
 * operations.
 * <p>
 * When remote receives a protocol request, it should first send back the same type protocol request to verify that it
 * understands that type of request. If it replies with {@link ProtocolRequest#None}, it will mean it doesn't support
 * that type of protocol request.
 * <p>
 * If remote replies with another type of protocol request it will mean the remote has prioritized that protocol
 * request, it should be executed over what is sent by you.
 */
public enum ProtocolRequest
{
    /**
     * Ineffective protocol request.
     */
    None(0),

    /**
     * Close the connection.
     * <p>
     * After this protocol request appears, the next read/write operation will certainly produce an error if you don't
     * quit the session.
     */
    Close(10),

    /**
     * Cancel the current operation.
     */
    Cancel(9),

    /*
     * Do an information exchange.
     */
    InfoExchange(8);

    public final int priority;

    /**
     * Create new instance.
     *
     * @param priority The priority that set the importance of execution.
     */
    ProtocolRequest(int priority)
    {
        this.priority = priority;
    }

    /**
     * Find the matching {@link ProtocolRequest} value using the given ordinal.
     *
     * @param ordinal For the expected protocol request object.
     * @return The found protocol request object, or the default {@link ProtocolRequest#None}.
     */
    public static @NotNull ProtocolRequest from(int ordinal)
    {
        ProtocolRequest[] values = values();
        if (ordinal < 0 || ordinal >= values.length)
            return None;

        return values[ordinal];
    }
}
