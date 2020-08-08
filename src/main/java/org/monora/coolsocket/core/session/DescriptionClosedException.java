package org.monora.coolsocket.core.session;

import org.monora.coolsocket.core.session.ActiveConnection.Description;

/**
 * Thrown when a closed {@link Description} is used to read/write data.
 */
public class DescriptionClosedException extends DescriptionException
{
    /**
     * Create a new instance where the description of the issue is provided.
     *
     * @param msg         The message explaining this error.
     * @param description The description object that was led to this error.
     */
    public DescriptionClosedException(String msg, Description description)
    {
        super(msg, description);
    }
}
