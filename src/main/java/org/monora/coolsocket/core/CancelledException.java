package org.monora.coolsocket.core;

import java.io.IOException;

/**
 * Thrown when one of the sides requests the cancellation of the operation.
 */
public class CancelledException extends IOException
{
    public final boolean remoteCancelled;

    /**
     * Create a new exception instance.
     *
     * @param message         the explanation for this error.
     * @param remoteCancelled true if this were requested by the remote, or false if it were requested by you.
     */
    public CancelledException(String message, boolean remoteCancelled)
    {
        super();
        this.remoteCancelled = remoteCancelled;
    }
}
