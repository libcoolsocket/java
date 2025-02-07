package org.monora.coolsocket.core.protocol;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Every error related to communication and this protocol will be a sub-class of this exception class.
 */
public class ProtocolException extends IOException {
    /**
     * Create a new instance
     *
     * @param message To describe the issue.
     */
    public ProtocolException(@Nullable String message) {
        super(message);
    }
}
