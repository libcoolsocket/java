package org.monora.coolsocket.core.response;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.monora.coolsocket.core.session.ActiveConnection;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.SocketAddress;

/**
 * This class represents the response received from the remote client that the CoolSocket is connected to.
 *
 * @see ActiveConnection#receive(OutputStream)
 */
public class Response
{
    /**
     * The remote that sent the response.
     */
    public final @NotNull SocketAddress remote;

    /**
     * The feature flags set for this part.
     */
    public final @NotNull Flags flags;

    /**
     * The length of the data received for this part. This will also be the total length of a chunked data.
     */
    public final long length;

    /**
     * If the received data is small in size, and is written to a byte buffer (e.g., {@link ByteArrayOutputStream}),
     * it will be included in this field.
     */
    public final @Nullable ByteArrayOutputStream data;

    /**
     * Creates a new Response instance.
     *
     * @param remote Where the remote is located at.
     * @param flags  The feature flags for this response.
     * @param length The total length of the data.
     * @param data   The byte data stored in the heap.
     */
    public Response(@NotNull SocketAddress remote, @NotNull Flags flags, long length,
                    @Nullable ByteArrayOutputStream data)
    {
        this.remote = remote;
        this.flags = flags;
        this.length = length;
        this.data = data;
    }

    /**
     * Check whether the data contains actual data.
     *
     * @return True if the data is actually an object.
     */
    public boolean containsData()
    {
        return data != null;
    }

    public @NotNull JSONObject getAsJson()
    {
        return new JSONObject(getAsString());
    }

    /**
     * Get the data as a JSON object.
     *
     * @param charsetName The name of the charset to use to decode the data.
     * @return The encapsulated JSON data.
     * @throws UnsupportedEncodingException If the supplied charset name is not available.
     */
    public @NotNull JSONObject getAsJson(@NotNull String charsetName) throws UnsupportedEncodingException
    {
        return new JSONObject(getAsString(charsetName));
    }

    /**
     * Return the data as a string.
     *
     * @param charsetName The name of the charset to use to decode the data.
     * @return The string representation of the {@link #data}.
     * @throws UnsupportedEncodingException If the supplied charset name is not available.
     * @see #getAsString()
     */
    public @NotNull String getAsString(@NotNull String charsetName) throws UnsupportedEncodingException
    {
        if (data == null) {
            throw new IllegalStateException("Trying to read data from a response whose data point is separate.");
        }
        return data.toString(charsetName);
    }

    /**
     * Get the data as a string.
     *
     * @return The string representation of the {@link #data}.
     * @see #getAsString(String)
     */
    public @NotNull String getAsString()
    {
        if (data == null) {
            throw new IllegalStateException("Trying to read data from a response whose data point is separate.");
        }
        return data.toString();
    }
}
