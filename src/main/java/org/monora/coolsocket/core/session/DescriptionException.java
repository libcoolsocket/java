package org.monora.coolsocket.core.session;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Thrown when a {@link ActiveConnection.Description} causes an error, or is used in wrong context.
 */
public class DescriptionException extends SessionException
{
    public final @NotNull ActiveConnection.Description description;

    /**
     * Create a new instance where the description of the issue is provided.
     *
     * @param msg         The message explaining this error.
     * @param description The description object that was led to this error.
     */
    public DescriptionException(@Nullable String msg, @NotNull ActiveConnection.Description description)
    {
        super(msg);
        this.description = description;
    }
}