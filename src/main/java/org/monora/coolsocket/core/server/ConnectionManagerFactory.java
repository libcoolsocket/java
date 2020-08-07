package org.monora.coolsocket.core.server;

/**
 * Connection manager factory handles the creation of {@link ConnectionManager} instances. Once a connection manager
 * is shutdown, it becomes unusable for next sessions due to the threads it may be maintaining being terminated.
 */
public interface ConnectionManagerFactory
{
    /**
     * Create a new connection manager that will be used by the session to accept client connections.
     *
     * @return The newly created connection manager instance.
     */
    ConnectionManager createConnectionManager();
}
