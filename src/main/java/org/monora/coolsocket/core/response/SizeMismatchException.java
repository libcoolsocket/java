package org.monora.coolsocket.core.response;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * This exception is thrown when a data length stays above or under the expected length.
 */
public abstract class SizeMismatchException extends IOException
{
    /**
     * This was the expected size
     */
    public final long sizeExpected;

    /**
     * This was what was given.
     */
    public final long sizeGot;

    /**
     * Create an instance where the expected and got size integers are given with a message with message describing the
     * issue.
     *
     * @param description  The message describing the issue.
     * @param sizeExpected The size that was expected.
     * @param sizeGot      The size that was given.
     */
    public SizeMismatchException(@Nullable String description, long sizeExpected, long sizeGot)
    {
        super(description);
        this.sizeExpected = sizeExpected;
        this.sizeGot = sizeGot;
    }
}
