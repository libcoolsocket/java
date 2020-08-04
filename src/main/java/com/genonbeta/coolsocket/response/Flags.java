package com.genonbeta.coolsocket.response;

public class Flags
{
    /**
     * Bit order: 0
     * <p>
     * This flags asks from the receiver to receive an info about the protocol and then send its own.
     */
    public static final int FLAG_INFO_EXCHANGE = 1;

    /**
     * Bit order: 1
     * <p>
     * Tells that the read is inconclusive and another read is needed. If the data is not chunked, then, the data will
     * be read as many as its length.
     */
    public static final int FLAG_DATA_CHUNKED = 1 << 1;

    private final long flags;

    public Flags(long flags)
    {
        this.flags = flags;
    }

    public long all()
    {
        return flags;
    }

    /**
     * Whether this data was received in chunks.
     *
     * @return true if the data was chunked.
     */
    public boolean chunked()
    {
        return (flags & FLAG_DATA_CHUNKED) != 0;
    }

    /**
     * This is a prior request from the one that writes to exchange information about the protocol or anything if the
     * user wants.
     *
     * @return true if this is an information exchange.
     */
    public boolean exchange()
    {
        return (flags & FLAG_INFO_EXCHANGE) != 0;
    }
}
