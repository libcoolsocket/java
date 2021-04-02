package org.monora.coolsocket.core.config;

/**
 * Stores info about the protocol and the library.
 */
public final class Config
{
    /**
     * The version number of the protocol.
     */
    public static final int PROTOCOL_VERSION = 1;

    /**
     * The default buffer size used when creating byte arrays that holds the buffer to exchanged data.
     */
    public static final int DEFAULT_BUFFER_SIZE = 8192;

    /**
     * The default inverse exchange point number.
     * <p>
     * This is the most performant and suitable cycle point for a fast enough connection.
     */
    public static final int DEFAULT_INVERSE_EXCHANGE_POINT = 2048;
}
