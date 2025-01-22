package org.monora.coolsocket.core.session;

import org.jetbrains.annotations.Nullable;

/**
 * Thrown when a closed {@link Channel.Descriptor} is used to read/write data.
 */
public class DescriptorClosedException extends DescriptorException {
    /**
     * Create a new instance where the description of the issue is provided.
     *
     * @param msg        The message explaining this error.
     * @param descriptor The description object that was led to this error.
     */
    public DescriptorClosedException(@Nullable String msg, Channel.Descriptor descriptor) {
        super(msg, descriptor);
    }
}
