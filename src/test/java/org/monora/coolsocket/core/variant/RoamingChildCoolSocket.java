package org.monora.coolsocket.core.variant;

import org.jetbrains.annotations.NotNull;
import org.monora.coolsocket.core.CoolSocket;
import org.monora.coolsocket.core.session.Channel;
import org.monora.coolsocket.core.variant.factory.TestConfigFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;

public class RoamingChildCoolSocket extends CoolSocket {
    public final BlockingQueue<Channel> connectionsQueue = new SynchronousQueue<>();

    public final boolean roamingEnabled;

    public RoamingChildCoolSocket(boolean roamingEnabled) {
        super(new TestConfigFactory());
        this.roamingEnabled = roamingEnabled;
    }

    @Override
    public void onConnected(@NotNull Channel channel) {
        if (roamingEnabled) {
            channel.setRoaming(true);
        }

        try {
            connectionsQueue.put(channel);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
