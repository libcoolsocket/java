package com.genonbeta.CoolSocket;

import org.json.JSONObject;

import java.net.SocketAddress;

/**
 * This class represents the response received from the opposite side that the CoolSocket
 * is connected to.
 *
 * @see ActiveConnection#receive()
 */
public class Response
{
    /**
     * The remote that sent the response.
     */
    public SocketAddress remote;

    /**
     * Header for the response.
     */
    public JSONObject header;

    /**
     * The response.
     *
     * @see Response#length
     */
    public String index;

    /**
     * The length of the response.
     */
    public long length = -1;

    /**
     * Creates an instance of this class. This does nothing for all the related
     * members are public.
     */
    public Response()
    {
    }
}
