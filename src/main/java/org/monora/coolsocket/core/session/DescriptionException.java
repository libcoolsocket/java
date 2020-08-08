package org.monora.coolsocket.core.session;

import java.io.IOException;

/**
 * Thrown when a {@link ActiveConnection.Description} causes an error, or is used in wrong context.
 */
public class DescriptionException extends IOException
{
    public final ActiveConnection.Description description;

    /**
     * Create a new instance where the description of the issue is provided.
     *
     * @param msg         The message explaining this error.
     * @param description The description object that was led to this error.
     */
    public DescriptionException(String msg, ActiveConnection.Description description)
    {
        super(msg);
        this.description = description;
    }
}