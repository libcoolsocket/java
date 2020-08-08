package org.monora.coolsocket.core.session;

/**
 * Thrown when one of the sides requests the cancellation of the operation.
 */
public class CancelledException extends SessionException
{
    public final boolean remoteRequested;

    /**
     * Create a new exception instance.
     *
     * @param message         The explanation for this error.
     * @param remoteRequested True if this were requested by the remote, or false if it were requested by you.
     */
    public CancelledException(String message, boolean remoteRequested)
    {
        super(message);
        this.remoteRequested = remoteRequested;
    }
}
