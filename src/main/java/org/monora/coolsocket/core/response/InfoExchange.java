package org.monora.coolsocket.core.response;

import org.jetbrains.annotations.NotNull;

/**
 * This enum encapsulates the type of info that can be exchanged. This is used when the protocol request
 * {@link ProtocolRequest#InfoExchange} is requested by any side of the communication.
 */
public enum InfoExchange {
    /**
     * The protocol version that this implementation supports.
     * <p>
     * Because they are expected to be supported, there isn't a separate field for minimum supported versions.
     * todo: Is this a good idea to not add minimum protocol version?
     */
    ProtocolVersion;

    /**
     * Finds the suitable instance for the given ordinal.
     *
     * @param ordinal The bit order of the info exchange
     * @return The matching enum.
     * @throws UnsupportedFeatureException If the request cannot be satisfied.
     */
    public static @NotNull InfoExchange from(int ordinal) throws UnsupportedFeatureException {
        InfoExchange[] values = values();
        if (ordinal < 0 || ordinal >= values.length)
            throw new UnsupportedFeatureException("Requested an unsupported exchange: " + ordinal);

        return values[ordinal];
    }
}
