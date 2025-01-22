package org.monora.coolsocket.core.variant;

import org.monora.coolsocket.core.session.Channel;
import org.monora.coolsocket.core.variant.factory.TestConfigFactory;

import java.io.IOException;
import java.net.Socket;

public class Connections {
    public static Channel open() throws IOException {
        Socket socket = new Socket();
        socket.connect(TestConfigFactory.SOCKET_ADDRESS);
        socket.setSoTimeout(TestConfigFactory.TIMEOUT_READ);
        return Channel.wrap(socket);
    }
}
