package org.monora.coolsocket.core.session;

import org.monora.coolsocket.core.session.ActiveConnection.Description;

/**
 * Thrown when wrong {@link Description} is used to read from or write to the remote.
 * <p>
 * In short, if the remote operation identity is different from ours.
 */
public class DescriptionMismatchException extends DescriptionException
{
    public final int remoteDescriptionId;

    /**
     * Create a new instance where the description of the issue is provided.
     *
     * @param msg                 The message explaining this error.
     * @param description         The description object that was led to this error.
     * @param remoteDescriptionId The description identity number reported by remote.
     */
    public DescriptionMismatchException(String msg, Description description, int remoteDescriptionId)
    {
        super(msg, description);
        this.remoteDescriptionId = remoteDescriptionId;
    }
}
