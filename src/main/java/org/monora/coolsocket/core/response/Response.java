package org.monora.coolsocket.core.response;

import org.monora.coolsocket.core.ActiveConnection;
import org.json.JSONObject;

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
    public final SocketAddress remote;

    /**
     * The feature flags set for this part.
     */
    public final Flags flags;

    /**
     * The length of the data received for this part. This will also be the total length of a chunked data.
     */
    public final long length;

    /**
     * If the received data is small in size, and is written to a byte buffer (e.g., {@link ByteArrayOutputStream}),
     * it will be included in this field.
     */
    public final ByteArrayOutputStream data;

    /**
     * Creates a new Response instance.
     *
     * @param remote where the remote is located at.
     * @param flags  the feature flags for this response.
     * @param length the total length of the data.
     * @param data   the byte data stored in RAM.
     */
    public Response(SocketAddress remote, long flags, long length, ByteArrayOutputStream data)
    {
        this.remote = remote;
        this.flags = new Flags(flags);
        this.length = length;
        this.data = data;
    }

    /**
     * Check whether the data contains actual data.
     *
     * @return true if the data is actually an object.
     */
    public boolean containsData()
    {
        return data != null;
    }

    public JSONObject getAsJson()
    {
        return new JSONObject(data.toString());
    }

    /**
     * Get the data as a JSON object.
     *
     * @param charsetName the name of the charset to use to decode the data.
     * @return the encapsulated JSON data.
     * @throws UnsupportedEncodingException if the supplied charset name is not available.
     */
    public JSONObject getAsJson(String charsetName) throws UnsupportedEncodingException
    {
        return new JSONObject(data.toString(charsetName));
    }

    /**
     * Return the data as a string.
     *
     * @param charsetName the name of the charset to use to decode the data.
     * @return the string representation of the {@link #data}.
     * @throws UnsupportedEncodingException if the supplied charset name is not available.
     * @see #getAsString()
     */
    public String getAsString(String charsetName) throws UnsupportedEncodingException
    {
        return data.toString(charsetName);
    }

    /**
     * Get the data as a string.
     *
     * @return the string representation of the {@link #data}.
     * @see #getAsString(String)
     */
    public String getAsString()
    {
        return data.toString();
    }
}
