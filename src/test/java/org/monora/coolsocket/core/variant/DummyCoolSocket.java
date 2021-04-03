package org.monora.coolsocket.core.variant;

import org.jetbrains.annotations.NotNull;
import org.monora.coolsocket.core.CoolSocket;
import org.monora.coolsocket.core.session.ActiveConnection;

/**
 * This is not meant to receive client requests.
 */
public class DummyCoolSocket extends CoolSocket
{
    public DummyCoolSocket(int port)
    {
        super(port);
    }

    @Override
    public void onConnected(@NotNull ActiveConnection activeConnection)
    {

    }
}
