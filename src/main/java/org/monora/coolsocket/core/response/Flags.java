package org.monora.coolsocket.core.response;

/**
 * This class encapsulates the flags that is in long integer format so that reading from it becomes an ordinary task.
 * <p>
 * This class is immutable, and if the modifying the flags is a need, you should create another instance for it.
 */
public class Flags {
    /**
     * The bit order: 0
     * <p>
     * Tells that the read is inconclusive and another read is needed. If the data is not chunked, then, the data will
     * be read as many as its length.
     */
    public static final int FLAG_DATA_CHUNKED = 1;

    /**
     * All the flags encapsulated by this instance.
     */
    public final long flags;

    /**
     * Create a new instance.
     *
     * @param flags That will be encapsulated.
     */
    public Flags(long flags) {
        this.flags = flags;
    }

    /**
     * Whether this data was received in chunks.
     *
     * @return True if the data was chunked.
     */
    public boolean chunked() {
        return (flags & FLAG_DATA_CHUNKED) != 0;
    }
}
