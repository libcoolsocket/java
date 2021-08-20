package org.monora.coolsocket.core.server;

import org.jetbrains.annotations.NotNull;
import org.monora.coolsocket.core.CoolSocket;
import org.monora.coolsocket.core.session.ActiveConnection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class DefaultConnectionManager implements ConnectionManager
{
    private final @NotNull List<@NotNull ActiveConnection> connectionList = new ArrayList<>();

    private final @NotNull ExecutorService executorService = Executors.newFixedThreadPool(10);

    private boolean waitForExit = true;

    private int closingContract = CLOSING_CONTRACT_DO_NOTHING;

    @Override
    public void closeAll()
    {
        if (connectionList.size() == 0 || executorService.isShutdown())
            return;

        int contract = closingContract;
        boolean wait = waitForExit;

        if (closingContract != CLOSING_CONTRACT_DO_NOTHING) {
            synchronized (connectionList) {
                for (ActiveConnection connection : connectionList) {
                    try {
                        switch (contract) {
                            case CLOSING_CONTRACT_CANCEL:
                                connection.cancel();
                                break;
                            case CLOSING_CONTRACT_CLOSE_SAFELY:
                                connection.closeSafely();
                                break;
                            case CLOSING_CONTRACT_CLOSE_IMMEDIATELY:
                            default:
                                connection.close();
                        }
                    } catch (IOException ignored) {
                    }
                }
            }
        }

        if (wait) {
            try {
                // Shutdown the threads.
                executorService.shutdown();
                // Wait for them to quit for 10 seconds at most.
                executorService.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
        }
    }

    @Override
    public void handleClient(@NotNull CoolSocket coolSocket, final @NotNull ActiveConnection activeConnection)
    {
        synchronized (connectionList) {
            connectionList.add(activeConnection);
        }

        executorService.submit(() -> {
            try {
                coolSocket.getClientHandler().onConnected(activeConnection);
            } catch (Exception e) {
                coolSocket.getLogger().log(Level.SEVERE, "An error occurred during handling of a client", e);
            } finally {
                if (!activeConnection.isRoaming()) {
                    try {
                        activeConnection.close();
                    } catch (IOException ignored) {
                    }
                }

                synchronized (connectionList) {
                    connectionList.remove(activeConnection);
                }
            }
        });
    }

    @Override
    public @NotNull List<@NotNull ActiveConnection> getActiveConnectionList()
    {
        return new ArrayList<>(connectionList);
    }

    @Override
    public void setClosingContract(boolean wait, int closingContract)
    {
        this.waitForExit = wait;
        this.closingContract = closingContract;
    }
}
