package org.monora.coolsocket.core.server;

import org.jetbrains.annotations.NotNull;
import org.monora.coolsocket.core.CoolSocket;
import org.monora.coolsocket.core.session.Channel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * The default connection manager implementation.
 */
public class DefaultConnectionManager implements ConnectionManager {
    /**
     * The list of managed connections.
     */
    private final @NotNull List<@NotNull Channel> connectionList = new ArrayList<>();

    /**
     * The executor service that offloads new connections to another thread.
     */
    private final @NotNull ExecutorService executorService = Executors.newFixedThreadPool(10);

    /**
     * Whether close functions will wait for all connections to close before returning.
     */
    private boolean waitForExit = true;

    /**
     * The contract on handling the connections while exiting/closing.
     */
    private int closingContract = CLOSING_CONTRACT_DO_NOTHING;

    /**
     * Close all the connections.
     */
    @Override
    public void closeAll() {
        if (connectionList.size() == 0 || executorService.isShutdown())
            return;

        int contract = closingContract;
        boolean wait = waitForExit;

        if (closingContract != CLOSING_CONTRACT_DO_NOTHING) {
            synchronized (connectionList) {
                for (Channel connection : connectionList) {
                    try {
                        switch (contract) {
                            case CLOSING_CONTRACT_CANCEL:
                                connection.cancel();
                                break;
                            case CLOSING_CONTRACT_CLOSE_SAFELY:
                                connection.closeMutually();
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

    /**
     * Handle the new connection.
     *
     * @param coolSocket The calling CoolSocket instance.
     * @param channel    To handle.
     */
    @Override
    public void handleClient(@NotNull CoolSocket coolSocket, final @NotNull Channel channel) {
        synchronized (connectionList) {
            connectionList.add(channel);
        }

        executorService.submit(() -> {
            try {
                coolSocket.getClientHandler().onConnected(channel);
            } catch (Exception e) {
                coolSocket.getLogger().log(Level.SEVERE, "An error occurred during handling of a client", e);
            } finally {
                if (!channel.isRoaming()) {
                    try {
                        channel.close();
                    } catch (IOException ignored) {
                    }
                }

                synchronized (connectionList) {
                    connectionList.remove(channel);
                }
            }
        });
    }

    /**
     * The list of connections.
     *
     * @return The list of connections.
     */
    @Override
    public @NotNull List<@NotNull Channel> getActiveConnectionList() {
        return new ArrayList<>(connectionList);
    }

    /**
     * Sets the closing contract to apply when closing.
     *
     * @param wait            True will mean {@link #closeAll()} will not return until all the connections stop.
     * @param closingContract The closing contract which will set how the manager will behave when {@link #closeAll()}
     *                        is invoked.
     */
    @Override
    public void setClosingContract(boolean wait, int closingContract) {
        this.waitForExit = wait;
        this.closingContract = closingContract;
    }
}
