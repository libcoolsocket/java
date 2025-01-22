package org.monora.coolsocket.core.session;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Thrown when a {@link Channel.Descriptor} causes an error, or is used in wrong context.
 */
public class DescriptorException extends SessionException {
    /**
     * The descriptor that caused the error.
     */
    public final @NotNull Channel.Descriptor descriptor;

    /**
     * Create a new instance where the description of the issue is provided.
     *
     * @param msg        The message explaining this error.
     * @param descriptor The description object that was led to this error.
     */
    public DescriptorException(@Nullable String msg, @NotNull Channel.Descriptor descriptor) {
        super(msg);
        this.descriptor = descriptor;
    }
}