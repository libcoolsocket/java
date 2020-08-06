package org.monora.coolsocket.core.response;

/**
 * Byte breaks are a way exchange information between two peers and make sure they are up to date on the changes about
 * each other.
 * <p>
 * They are also sent and received to ensure the bidirectional communication between the two sides still occurs even
 * when the one directional read-write is the case, i.e. when you're writing but not reading at all.
 * <p>
 * They are also exchanged during the beginning of a read or write operation, or when there is a read and write
 * operation taking place.
 * <p>
 * The cancel request, for instance, is a byte break operation that happens during any of the read or write operations.
 * <p>
 * When remote receives a byte break request, it should first send back the same type byte break to verify that it
 * understands that that type of request. If it replies with {@link ByteBreak#None}, it will mean it doesn't support
 * that type of byte break.
 * <p>
 * If remote replies with another type of byte break it will mean the remote has prioritized that byte break, it should
 * be executed over what is sent by you.
 */
public enum ByteBreak
{
    /**
     * Ineffective byte break.
     */
    None,

    /*
     * Asks from the receiver to receive an info about the protocol and then send its own.
     */
    InfoExchange,

    /**
     * Close the connection
     *
     * When this byte break appears the next read/write operation will certainly produce an error if you don't quit
     * the session.
     */
    Close,

    /**
     * Cancel the current operation.
     */
    Cancel;

    /**
     * Find the matching {@link ByteBreak} value using the given ordinal.
     *
     * @param ordinal For the expected byte break object.
     * @return The found byte break object, or the default {@link ByteBreak#None}.
     */
    public static ByteBreak from(int ordinal)
    {
        ByteBreak[] values = values();
        if (ordinal < 0 || ordinal >= values.length)
            return ByteBreak.None;

        return values[ordinal];
    }
}
