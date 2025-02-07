package org.monora.coolsocket.core.session;

import org.jetbrains.annotations.Nullable;

/**
 * Thrown when one of the sides requests the cancellation of the operation.
 */
public class CancelledException extends SessionException {
    /**
     * Whether the cancellation was requested by the remote.
     */
    public final boolean remoteRequested;

    /**
     * Create a new exception instance.
     *
     * @param message         The explanation for this error.
     * @param remoteRequested True if this were requested by the remote, or false if it were requested by you.
     */
    public CancelledException(@Nullable String message, boolean remoteRequested) {
        super(message);
        this.remoteRequested = remoteRequested;
    }
}
