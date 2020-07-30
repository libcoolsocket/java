package com.genonbeta.coolsocket;

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
     * The properties of this connection defined in bit order.
     *
     * @see CoolSocket#FLAG_DATA_CHUNKED
     */
    public final int flags;

    /**
     * The length of the header.
     */
    public final short headerLength;

    /**
     * Header for the response.
     *
     * @see #headerLength
     */
    public final ByteArrayOutputStream header;

    /**
     * The length of the index.
     */
    public final int indexLength;

    /**
     * The response.
     *
     * @see #indexLength
     */
    public final ByteArrayOutputStream index;

    /**
     * Creates a new Response instance.
     *
     * @param remote       where the remote is located at.
     * @param headerLength the total length of the header.
     * @param indexLength  the total length of the index.
     * @param flags        the feature flags specifying which features were used for this response.
     * @param header       the header data.
     * @param index        the index data.
     */
    Response(SocketAddress remote, int flags, short headerLength, int indexLength, ByteArrayOutputStream header,
             ByteArrayOutputStream index)
    {
        this.remote = remote;
        this.flags = flags;
        this.headerLength = headerLength;
        this.indexLength = indexLength;
        this.header = header;
        this.index = index;
    }

    /**
     * Get the header part as a JSON object.
     *
     * @param charset to use to decode the header data.
     * @return the encapsulated JSON data.
     * @throws UnsupportedEncodingException when the given charset to use as encoding is not known to the system.
     */
    public JSONObject getHeaderAsJson(String charset) throws UnsupportedEncodingException
    {
        return new JSONObject(header.toString(charset));
    }

    /**
     * Return the index part of the data as a string if it exists. Will return a null pointer exception if its null.
     *
     * @return the string representation of the {@link #index} decoded with the default charset
     * @see #getIndexAsString(String)
     * @see #hasBody()
     */
    public String getIndexAsString()
    {
        return index.toString();
    }

    /**
     * Return the index part of the data as a string decoded using a user-preferred charset.
     *
     * @param charsetName the index is encoded with
     * @return the string representation of the {@link #index}
     * @throws UnsupportedEncodingException as per defined by {@link ByteArrayOutputStream#toString(String)}
     * @see #getIndexAsString()
     * @see #hasBody()
     */
    public String getIndexAsString(String charsetName) throws UnsupportedEncodingException
    {
        return index.toString(charsetName);
    }

    /**
     * Check whether this response instance has a body.
     *
     * @return true if it has a body, or false if otherwise.
     * @see #getIndexAsString()
     */
    public boolean hasBody()
    {
        return index != null;
    }

    /**
     * Whether this data was received in chunks.
     *
     * @return true if the data was chunked.
     */
    public boolean isDataChunked()
    {
        return (flags & CoolSocket.FLAG_DATA_CHUNKED) != 0;
    }

    /**
     * Same as {@link Response#getIndexAsString()}.
     *
     * @return the index as string.
     * @see #getIndexAsString()
     * @see #hasBody()
     */
    @Override
    public String toString()
    {
        return getIndexAsString();
    }
}
