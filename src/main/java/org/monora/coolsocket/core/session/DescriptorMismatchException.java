package org.monora.coolsocket.core.session;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Thrown when wrong {@link Channel.Descriptor} is used to read from or write to the remote.
 * <p>
 * In short, if the remote operation identity is different from ours.
 */
public class DescriptorMismatchException extends DescriptorException {
    /**
     * The unique id of the descriptor.
     */
    public final int remoteDescriptionId;

    /**
     * Create a new instance where the description of the issue is provided.
     *
     * @param msg                 The message explaining this error.
     * @param descriptor          The description object that was led to this error.
     * @param remoteDescriptionId The description identity number reported by remote.
     */
    public DescriptorMismatchException(@Nullable String msg, @NotNull Channel.Descriptor descriptor,
                                       int remoteDescriptionId) {
        super(msg, descriptor);
        this.remoteDescriptionId = remoteDescriptionId;
    }
}
