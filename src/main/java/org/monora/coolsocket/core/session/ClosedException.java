package org.monora.coolsocket.core.session;

/**
 * This error is thrown when the remote or us want to close the connection. Once thrown, the next read/write operation
 * will be invalid and won't work. Immediate closure of the connection and the session is needed.
 * <p>
 * The difference between this and closing the connection abruptly is the latter doesn't inform the other side that
 * it was requested by the peer.
 */
public class ClosedException extends CancelledException
{
    /**
     * Create a new exception instance.
     *
     * @param message         The explanation for this error.
     * @param remoteCancelled True if this were requested by the remote, or false if it were requested by you.
     */
    public ClosedException(String message, boolean remoteCancelled)
    {
        super(message, remoteCancelled);
    }
}
