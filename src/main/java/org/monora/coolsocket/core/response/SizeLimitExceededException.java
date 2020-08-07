package org.monora.coolsocket.core.response;

/**
 * This exception is thrown when the given size is larger than what was expected.
 */
public class SizeLimitExceededException extends SizeMismatchException
{
    /**
     * Create an instance where the expected and got size integers are given with a message with message describing the
     * issue.
     *
     * @param description  The message describing the issue.
     * @param sizeExpected The size that was expected.
     * @param sizeGot      The size that was given.
     */
    public SizeLimitExceededException(String description, long sizeExpected, long sizeGot)
    {
        super(description, sizeExpected, sizeGot);
    }
}
