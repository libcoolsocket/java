package org.monora.coolsocket.core.server;

import org.jetbrains.annotations.NotNull;
import org.monora.coolsocket.core.CoolSocket;
import org.monora.coolsocket.core.session.ActiveConnection;

import java.net.InetAddress;
import java.util.List;

/**
 * This class type is used to handle the connections and how they will live whilst they are alive.
 */
public interface ConnectionManager
{
    /**
     * Close the connections as soon as {@link #closeAll()} is invoked.
     */
    int CLOSING_CONTRACT_CLOSE_IMMEDIATELY = 1;

    /**
     * Close using {@link ActiveConnection#closeSafely()} which will inform the remote and close the connection.
     */
    int CLOSING_CONTRACT_CLOSE_SAFELY = 2;

    /**
     * Cancel the ongoing operations informing the remote about it using {@link ActiveConnection#cancel()}. This doesn't
     * guarantee that the connections will be closed. If both sides of a connection want, the existing connection can
     * still be maintained after the cancel request is handled.
     */
    int CLOSING_CONTRACT_CANCEL = 4;

    /**
     * Do nothing about the connections to the server. They can exit whenever they want.
     */
    int CLOSING_CONTRACT_DO_NOTHING = 8;

    /**
     * Close all the client connections following the contract type that was set.
     *
     * @see #setClosingContract(boolean, int)
     */
    void closeAll();

    /**
     * Handle a client connection assigning it to a thread that will execute it asynchronously.
     *
     * @param coolSocket       The calling CoolSocket instance.
     * @param activeConnection To handle.
     */
    void handleClient(@NotNull CoolSocket coolSocket, @NotNull ActiveConnection activeConnection);

    /**
     * Returns the list of active connections that are still ongoing. This list does not hold the connections that are
     * closed.
     *
     * @return A copy list of active connections that are still alive.
     */
    List<@NotNull ActiveConnection> getActiveConnectionList();

    /**
     * Counts the total connection of a client to the CoolSocket server.
     *
     * @param address Client address.
     * @return Total number of connections.
     */
    default int getConnectionCountByAddress(@NotNull InetAddress address)
    {
        int returnObject = 0;

        for (ActiveConnection activeConnection : getActiveConnectionList())
            if (activeConnection.getAddress().equals(address))
                returnObject++;

        return returnObject;
    }

    /**
     * Set the closing contract for this connection manager.
     * <p>
     * This will be effective for the existing connections and the upcoming ones.
     *
     * @param wait            True will mean {@link #closeAll()} will not return until all the connections stop.
     * @param closingContract The closing contract which will set how the manager will behave when {@link #closeAll()}
     *                        is invoked.
     * @see #CLOSING_CONTRACT_CLOSE_IMMEDIATELY
     * @see #CLOSING_CONTRACT_CLOSE_SAFELY
     * @see #CLOSING_CONTRACT_CANCEL
     * @see #CLOSING_CONTRACT_DO_NOTHING
     */
    void setClosingContract(boolean wait, int closingContract);
}
