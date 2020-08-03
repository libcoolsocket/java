package com.genonbeta.coolsocket.server;

import com.genonbeta.coolsocket.ActiveConnection;
import com.genonbeta.coolsocket.CoolSocket;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public class DefaultConnectionManager implements ConnectionManager
{
    private final List<ActiveConnection> connectionList = new ArrayList<>();

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    @Override
    public void closeAll()
    {
        if (connectionList.size() == 0)
            return;

        List<ActiveConnection> connections = new ArrayList<>(connectionList);
        for (ActiveConnection connection : connections)
            try {
                connection.close();
            } catch (IOException ignored) {
            }
    }

    @Override
    public void handleClient(CoolSocket coolSocket, final ActiveConnection activeConnection)
    {
        synchronized (connectionList) {
            connectionList.add(activeConnection);
        }

        executorService.submit(() -> {
            try {
                coolSocket.onConnected(activeConnection);
            } catch (Exception e) {
                coolSocket.getLogger().log(Level.SEVERE, "An error occurred during handling of a client", e);
            } finally {
                synchronized (connectionList) {
                    IOUtils.closeQuietly(activeConnection);
                    connectionList.remove(activeConnection);
                }
            }
        });
    }

    @Override
    public List<ActiveConnection> getActiveConnectionList()
    {
        return new ArrayList<>(connectionList);
    }
}
