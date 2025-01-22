package org.monora.coolsocket.core.response;

import org.jetbrains.annotations.Nullable;
import org.monora.coolsocket.core.protocol.ProtocolException;

/**
 * Thrown when an unsupported feature is requested, indicating it shouldn't have been requested and that it is an
 * error.
 */
public class UnsupportedFeatureException extends ProtocolException {
    /**
     * Create a new instance
     *
     * @param message To describe the issue.
     */
    public UnsupportedFeatureException(@Nullable String message) {
        super(message);
    }
}
