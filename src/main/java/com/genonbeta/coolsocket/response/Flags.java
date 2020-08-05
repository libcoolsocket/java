package com.genonbeta.coolsocket.response;

public class Flags
{
    /**
     * Bit order: 0
     * <p>
     * Tells that the read is inconclusive and another read is needed. If the data is not chunked, then, the data will
     * be read as many as its length.
     */
    public static final int FLAG_DATA_CHUNKED = 1;

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
}
