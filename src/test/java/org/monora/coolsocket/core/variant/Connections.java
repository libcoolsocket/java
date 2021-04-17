package org.monora.coolsocket.core.variant;

import org.monora.coolsocket.core.session.ActiveConnection;
import org.monora.coolsocket.core.variant.factory.TestConfigFactory;

import java.io.IOException;

public class Connections
{
    public static ActiveConnection connect() throws IOException
    {
        return ActiveConnection.connect(TestConfigFactory.SOCKET_ADDRESS, TestConfigFactory.TIMEOUT_READ);
    }
}
