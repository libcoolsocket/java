package org.monora.coolsocket.core.session;

import org.jetbrains.annotations.Nullable;
import org.monora.coolsocket.core.protocol.ProtocolException;

/**
 * This error type represents any error that concerns the session (or active connection).
 * <p>
 * Something unexpected wants the session to change direction either by doing something else, or not doing anything at
 * all, i.e. the ongoing operation gets cancelled.
 */
public class SessionException extends ProtocolException {
    /**
     * Create a new exception instance.
     *
     * @param message The explanation for this error.
     */
    public SessionException(@Nullable String message) {
        super(message);
    }
}
