package org.monora.coolsocket.core.response;

/**
 * This enum encapsulates the type of info that can be exchanged. This is used when the byte break
 * {@link ByteBreak#InfoExchange} is requested by any side of the communication.
 */
public enum InfoExchange
{
    /**
     * This is not meant for real world usage. Even if you use this, the results may be incomplete due to it being a
     * placeholder.
     */
    Dummy(Integer.MAX_VALUE),

    /**
     * The protocol version is used to identify the client features and act in an appropriate manner so that the
     * communication can be established even when one of the clients is old.
     */
    ProtocolVersion(Short.MAX_VALUE);

    /**
     * The max length for this type of information exchange.
     */
    public final int maxLength;

    InfoExchange(int maxLength)
    {
        this.maxLength = maxLength;
    }

    public static InfoExchange from(int ordinal) {
        InfoExchange[] values = values();
        if (ordinal < 0 || ordinal >= values.length)
            return InfoExchange.Dummy;

        return values[ordinal];
    }
}
