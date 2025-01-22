package org.monora.coolsocket.core.response;

import org.jetbrains.annotations.NotNull;
import org.monora.coolsocket.core.session.Channel;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.SocketAddress;

/**
 * This class represents the response received from the remote client that the CoolSocket is connected to.
 *
 * @see Channel#readAll
 */
public class Response {
    /**
     * The remote that sent the response.
     */
    public final @NotNull SocketAddress remote;

    /**
     * The flags set for the response.
     */
    public final @NotNull Flags flags;

    /**
     * The total length of the data.
     */
    public final long length;

    /**
     * The data.
     */
    public final @NotNull ByteArrayOutputStream data;

    /**
     * Creates a new Response instance.
     *
     * @param remote Where the remote is located at.
     * @param flags  The feature flags for this response.
     * @param length The total length of the data.
     * @param data   The data.
     */
    public Response(@NotNull SocketAddress remote, @NotNull Flags flags, long length,
                    @NotNull ByteArrayOutputStream data) {
        this.remote = remote;
        this.flags = flags;
        this.length = length;
        this.data = data;
    }

    /**
     * Return the data as a string.
     *
     * @param charsetName The name of the charset to use to decode the data.
     * @return The string representation of the {@link #data}.
     * @throws UnsupportedEncodingException If the supplied charset name is not available.
     * @see #getAsString()
     */
    public @NotNull String getAsString(@NotNull String charsetName) throws UnsupportedEncodingException {
        return data.toString(charsetName);
    }

    /**
     * Get the data as a string.
     *
     * @return The string representation of the {@link #data}.
     * @see #getAsString(String)
     */
    public @NotNull String getAsString() {
        return data.toString();
    }

    /**
     * Get the raw data.
     *
     * @return The raw data.
     */
    public @NotNull byte[] getBytes() {
        return data.toByteArray();
    }
}
