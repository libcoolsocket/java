package org.monora.coolsocket.core.response;

import org.jetbrains.annotations.Nullable;

/**
 * This exception is thrown when the given size is larger than what was expected.
 */
public class SizeOverflowException extends SizeMismatchException
{
    /**
     * Create an instance where the expected and got size integers are given with a message with message describing the
     * issue.
     *
     * @param description  The message describing the issue.
     * @param sizeExpected The size that was expected.
     * @param sizeGot      The size that was given.
     */
    public SizeOverflowException(@Nullable String description, long sizeExpected, long sizeGot)
    {
        super(description, sizeExpected, sizeGot);
    }
}
