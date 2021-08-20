package org.monora.coolsocket.core.variant;

import org.jetbrains.annotations.NotNull;
import org.monora.coolsocket.core.CoolSocket;
import org.monora.coolsocket.core.session.ActiveConnection;
import org.monora.coolsocket.core.variant.factory.TestConfigFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;

public class RoamingChildCoolSocket extends CoolSocket
{
    public final BlockingQueue<ActiveConnection> connectionsQueue = new SynchronousQueue<>();

    public final boolean roamingEnabled;

    public RoamingChildCoolSocket(boolean roamingEnabled)
    {
        super(new TestConfigFactory());
        this.roamingEnabled = roamingEnabled;
    }

    @Override
    public void onConnected(@NotNull ActiveConnection activeConnection)
    {
        if (roamingEnabled) {
            activeConnection.setRoaming(true);
        }

        try {
            connectionsQueue.put(activeConnection);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
