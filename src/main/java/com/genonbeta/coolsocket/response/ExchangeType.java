package com.genonbeta.coolsocket.response;

/**
 * This enum encapsulates the info types that can be exchanged.
 * {@link Flags#FLAG_INFO_EXCHANGE}.
 */
public enum Feature
{
    Dummy(Integer.MAX_VALUE),
    ProtocolVersion(Short.MAX_VALUE);

    int maxLength;

    Feature(int maxLength)
    {
        this.maxLength = maxLength;
    }

    public int getMaxLength()
    {
        return maxLength;
    }
}
