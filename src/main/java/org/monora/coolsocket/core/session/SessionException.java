package org.monora.coolsocket.core.session;

import java.io.IOException;

/**
 * This error type represents any error that concerns the session (or active connection).
 * <p>
 * Something unexpected wants the session to change direction either by doing something else, or not doing anything at
 * all, i.e. the ongoing operation gets cancelled.
 */
public class SessionException extends IOException
{
    public SessionException()
    {
        super();
    }
}
