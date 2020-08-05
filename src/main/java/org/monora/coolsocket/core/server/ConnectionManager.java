package org.monora.coolsocket.core.server;

import org.monora.coolsocket.core.ActiveConnection;
import org.monora.coolsocket.core.CoolSocket;

import java.net.InetAddress;
import java.util.List;

public interface ConnectionManager
{
    /**
     * Force close all the active connections.
     */
    void closeAll();

    /**
     * Handle a client connection assigning it to a thread that will execute it asynchronously.
     *
     * @param coolSocket The calling CoolSocket instance.
     * @param activeConnection To handle.
     */
    void handleClient(CoolSocket coolSocket, ActiveConnection activeConnection);

    /**
     * Returns the list of active connections that are still ongoing. This list does not hold the connections that are
     * closed.
     *
     * @return A copy list of active connections that are still alive.
     */
    List<ActiveConnection> getActiveConnectionList();

    /**
     * Counts the total connection of a client to the CoolSocket server.
     *
     * @param address Client address.
     * @return Total number of connections.
     */
    default int getConnectionCountByAddress(InetAddress address)
    {
        int returnObject = 0;

        for (ActiveConnection activeConnection : getActiveConnectionList())
            if (activeConnection.getAddress().equals(address))
                returnObject++;

        return returnObject;
    }
}
