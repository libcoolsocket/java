package com.genonbeta.coolsocket;

import java.io.IOException;

/**
 * This exception is thrown when a data length exceeds the maximum length allowed.
 */
public class SizeExceededException extends IOException
{
    public SizeExceededException()
    {
        super();
    }

    public SizeExceededException(String s)
    {
        super(s);
    }

    public SizeExceededException(String s, Throwable throwable)
    {
        super(s, throwable);
    }

    public SizeExceededException(Throwable throwable)
    {
        super(throwable);
    }
}
